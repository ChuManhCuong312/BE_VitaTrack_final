package com.vitatrack.service.impl;

import com.vitatrack.entity.HealthAlert;
import com.vitatrack.entity.HealthProfile;
import com.vitatrack.repository.ActivityRepository;
import com.vitatrack.repository.HealthAlertRepository;
import com.vitatrack.repository.HealthProfileRepository;
import com.vitatrack.repository.MealRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-09 / UC-16 – Health Alert Engine
 * 9 loại rule (đầy đủ theo SDD 6.2.3):
 *   BMI_UNDERWEIGHT, BMI_OVERWEIGHT, BMI_OBESE,
 *   CALORIE_OVERAGE, CALORIE_DEFICIT, NO_LOG_TODAY,
 *   LOW_PROTEIN, SLEEP_DEFICIT, TACHYCARDIA / BRADYCARDIA
 * 4 mức severity: LOW | MEDIUM | HIGH | CRITICAL
 * Cooldown 24h chống spam.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final HealthAlertRepository alertRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final MealRepository mealRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    public List<HealthAlert> getAlerts(Long userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<HealthAlert> getUnread(Long userId) {
        return alertRepository.findByUserIdAndReadFalse(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return alertRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public HealthAlert markRead(Long userId, Long alertId) {
        HealthAlert a = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Cảnh báo không tồn tại."));
        if (!a.getUser().getId().equals(userId))
            throw new RuntimeException("Không có quyền.");
        a.setRead(true);
        return alertRepository.save(a);
    }

    @Override
    public void markAllRead(Long userId) {
        alertRepository.findByUserIdAndReadFalse(userId).forEach(a -> {
            a.setRead(true);
            alertRepository.save(a);
        });
    }

    @Override
    public void evaluate(Long userId) {
        HealthProfile p = healthProfileRepository.findByUserId(userId).orElse(null);
        if (p == null) return;

        List<HealthAlert> toCreate = new ArrayList<>();
        LocalDate today = LocalDate.now();

        double caloriesIn = mealRepository.sumCaloriesByUserAndDate(userId, today);
        int    goal       = p.getDailyCalorieGoal() != null ? p.getDailyCalorieGoal() : 2000;

        // ─── RULE 1: BMI_UNDERWEIGHT ──────────────────────────────────────
        if (p.getBmi() != null && p.getBmi() < 18.5) {
            maybeCreate(userId, "BMI_UNDERWEIGHT", "HIGH",
                String.format("BMI của bạn là %.1f (thiếu cân). Hãy tăng khẩu phần ăn và tham khảo chuyên gia dinh dưỡng.",
                    p.getBmi()),
                p.getBmi(), 18.5, toCreate);
        }

        // ─── RULE 2: BMI_OVERWEIGHT ───────────────────────────────────────
        if (p.getBmi() != null && p.getBmi() >= 25.0 && p.getBmi() < 30.0) {
            maybeCreate(userId, "BMI_OVERWEIGHT", "MEDIUM",
                String.format("BMI của bạn là %.1f (thừa cân). Hãy điều chỉnh chế độ ăn và tăng vận động.",
                    p.getBmi()),
                p.getBmi(), 25.0, toCreate);
        }

        // ─── RULE 3: BMI_OBESE ────────────────────────────────────────────
        if (p.getBmi() != null && p.getBmi() >= 30.0) {
            maybeCreate(userId, "BMI_OBESE", "HIGH",
                String.format("BMI của bạn là %.1f (béo phì). Cần can thiệp y tế và dinh dưỡng ngay.",
                    p.getBmi()),
                p.getBmi(), 30.0, toCreate);
        }

        // ─── RULE 4: CALORIE_OVERAGE (>120% mục tiêu) ────────────────────
        if (caloriesIn > goal * 1.2) {
            maybeCreate(userId, "CALORIE_OVERAGE", "MEDIUM",
                String.format("Hôm nay bạn đã nạp %.0f kcal, vượt %.0f kcal so với mục tiêu %d kcal.",
                    caloriesIn, caloriesIn - goal, goal),
                caloriesIn, goal * 1.2, toCreate);
        }

        // ─── RULE 5: CALORIE_DEFICIT (<50% mục tiêu sau 20h) ─────────────
        if (java.time.LocalTime.now().getHour() >= 20 && caloriesIn < goal * 0.5) {
            maybeCreate(userId, "CALORIE_DEFICIT", "LOW",
                String.format("Bạn chỉ nạp %.0f kcal hôm nay — dưới 50%% mục tiêu. Hãy ăn thêm bữa tối.",
                    caloriesIn),
                caloriesIn, goal * 0.5, toCreate);
        }

        // ─── RULE 6: NO_LOG_TODAY ─────────────────────────────────────────
        if (caloriesIn == 0) {
            maybeCreate(userId, "NO_LOG_TODAY", "LOW",
                "Bạn chưa ghi nhật ký bữa ăn nào hôm nay. Hãy bắt đầu theo dõi dinh dưỡng của bạn!",
                0.0, 1.0, toCreate);
        }

        // ─── RULE 7: LOW_PROTEIN (<0.8g/kg cân nặng, chỉ tính nếu đã log bữa ăn) ──
        if (caloriesIn > 0 && p.getWeight() != null && p.getWeight() > 0) {
            // Lấy tổng protein hôm nay
            List<com.vitatrack.entity.MealLog> todayMeals =
                mealRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, today);
            double totalProtein = todayMeals.stream()
                .mapToDouble(m -> m.getProtein() != null ? m.getProtein() : 0).sum();
            double minProtein = p.getWeight() * 0.8; // 0.8g/kg là mức tối thiểu WHO

            if (totalProtein < minProtein) {
                maybeCreate(userId, "LOW_PROTEIN", "MEDIUM",
                    String.format("Protein hôm nay %.1fg – thấp hơn mức tối thiểu %.1fg (0.8g/kg cân nặng). Hãy bổ sung thực phẩm giàu đạm.",
                        totalProtein, minProtein),
                    totalProtein, minProtein, toCreate);
            }
        }

        // ─── RULE 8 & 9: SLEEP_DEFICIT, TACHYCARDIA / BRADYCARDIA ───────────────
        // Lấy log activity gần nhất có sleep/heartRate data
        activityRepository.findLatestWithSleep(userId).ifPresent(act -> {
            if (act.getSleepHours() != null && act.getSleepHours() < 6.0) {
                maybeCreate(userId, "SLEEP_DEFICIT", "HIGH",
                    String.format("Bạn chỉ ngủ %.1f giờ đêm qua — ít hơn mức khuyến nghị 7-9 giờ. Thiếu ngủ ảnh hưởng đến trao đổi chất.",
                        act.getSleepHours()),
                    act.getSleepHours(), 6.0, toCreate);
            }
        });

        activityRepository.findLatestWithHeartRate(userId).ifPresent(act -> {
            if (act.getHeartRateAvg() != null) {
                int hr = act.getHeartRateAvg();
                if (hr > 100) {
                    maybeCreate(userId, "TACHYCARDIA", "HIGH",
                        String.format("Nhịp tim trung bình của bạn là %d bpm (nhịp tim nhanh > 100 bpm). Hãy nghỉ ngơi và tham khảo bác sĩ nếu kéo dài.",
                            hr),
                        (double) hr, 100.0, toCreate);
                } else if (hr < 50) {
                    maybeCreate(userId, "BRADYCARDIA", "HIGH",
                        String.format("Nhịp tim trung bình của bạn là %d bpm (nhịp tim chậm < 50 bpm). Hãy tham khảo bác sĩ.",
                            hr),
                        (double) hr, 50.0, toCreate);
                }
            }
        });

        toCreate.forEach(alertRepository::save);
        log.info("[AlertEngine] userId={} evaluated, {} new alerts created", userId, toCreate.size());
    }

    // ── FR-32.Acknowledge ─────────────────────────────────────────────────

    /**
     * Expert đánh dấu alert "Đã xử lý" + ghi chú hành động.
     * Lịch sử tất cả acknowledge được lưu lại (FR-32.Acknowledge).
     */
    @Override
    public void acknowledge(Long expertUserId, Long alertId, String note) {
        HealthAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Cảnh báo không tồn tại."));

        alert.setAcknowledged(true);
        alert.setAcknowledgedNote(note);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setRead(true);
        alertRepository.save(alert);
        log.info("[Alert] Acknowledged alertId={} by expertUserId={}", alertId, expertUserId);
    }

    // ── Helper: tạo alert nếu chưa có cùng type trong 24h (cooldown chống spam) ──
    private void maybeCreate(Long userId, String type, String severity,
                              String message, double triggered, double threshold,
                              List<HealthAlert> sink) {
        boolean exists = alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(a -> a.getAlertType().equals(type)
                        && a.getCreatedAt() != null
                        && a.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)));
        if (!exists) {
            sink.add(HealthAlert.builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .alertType(type)
                    .severity(severity)
                    .message(message)
                    .triggeredValue(triggered)
                    .thresholdValue(threshold)
                    .build());
        }
    }
}
