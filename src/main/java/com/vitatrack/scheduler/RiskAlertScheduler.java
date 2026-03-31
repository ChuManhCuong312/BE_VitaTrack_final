package com.vitatrack.scheduler;

import com.vitatrack.entity.HealthAlert;
import com.vitatrack.entity.HealthProfile;
import com.vitatrack.entity.User;
import com.vitatrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RiskAlertScheduler – FR-32: Phát Risk Alert tự động theo SRS.
 *
 * Các trigger được implement:
 *
 *  FR-32.TriggerLowCalorie   23:59 mỗi ngày – Calories_In < 50% TDEE → tăng counter.
 *                            Counter = 3 liên tiếp → phát alert "Ăn ít calo liên tục".
 *
 *  FR-32.TriggerHighCalorie  23:59 mỗi ngày – Calories_In > 150% Daily_Calorie_Target
 *                            trong 3 ngày liên tiếp → phát alert "Ăn vượt calo liên tục".
 *
 *  FR-32.TriggerBMI          Trigger ngay khi user cập nhật cân nặng (xử lý trong
 *                            HealthProfileServiceImpl.triggerBmiAlertIfNeeded).
 *                            Scheduler chỉ backup scan daily cho những user không login.
 *
 *  FR-32.Notification        Gửi push notification + email đến chuyên gia khi phát alert.
 *
 *  FR-32.UserNotify          Gửi thông báo nhẹ đến người dùng (không tiết lộ nội dung alert).
 *
 *  Cleanup                   Xóa alert cũ hơn 90 ngày – 02:00 mỗi ngày.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAlertScheduler {

    // ── Ngưỡng nghiệp vụ (SRS FR-32) ─────────────────────────────────────
    private static final double LOW_CALORIE_THRESHOLD_PCT  = 0.50;  // < 50% TDEE
    private static final double HIGH_CALORIE_THRESHOLD_PCT = 1.50;  // > 150% daily target
    private static final int    CONSECUTIVE_DAYS_TRIGGER   = 3;
    private static final double BMI_CRITICAL_LOW            = 17.0;
    private static final double BMI_OBESE                   = 30.0;

    private final UserRepository             userRepository;
    private final HealthProfileRepository    healthProfileRepository;
    private final MealRepository             mealRepository;
    private final HealthAlertRepository      alertRepository;
    private final ExpertAssignmentRepository expertAssignmentRepository;
    private final JavaMailSender             mailSender;

    // ── FR-32 Daily Evaluation – 23:59 mỗi ngày ──────────────────────────

    /**
     * Chạy lúc 23:59 mỗi ngày.
     * Với mỗi user active có health profile đầy đủ:
     *  1. Kiểm tra calorie thấp liên tiếp (FR-32.TriggerLowCalorie)
     *  2. Kiểm tra calorie cao liên tiếp (FR-32.TriggerHighCalorie)
     *  3. Backup BMI check (FR-32.TriggerBMI)
     */
    @Scheduled(cron = "0 59 23 * * *")
    @Transactional
    public void evaluateDailyRisks() {
        log.info("[RiskAlert] Daily evaluation started at {}", LocalDateTime.now());

        List<User> activeUsers = userRepository.findAllActiveWithHealthProfile();
        int alertCount = 0;

        for (User user : activeUsers) {
            try {
                alertCount += evaluateUser(user);
            } catch (Exception e) {
                log.warn("[RiskAlert] Failed for userId={}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("[RiskAlert] Daily evaluation completed. {} new alerts created.", alertCount);
    }

    // ── Cleanup – 02:00 mỗi ngày ─────────────────────────────────────────

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = alertRepository.deleteOlderThan(cutoff);
        log.info("[RiskAlert] Cleanup: {} old alerts deleted (older than 90 days)", deleted);
    }

    // ── Per-user evaluation ───────────────────────────────────────────────

    private int evaluateUser(User user) {
        HealthProfile profile = healthProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null || profile.getTdee() == null) return 0;

        int created = 0;
        LocalDate today = LocalDate.now();

        // ─── FR-32.TriggerLowCalorie ──────────────────────────────────────
        created += checkLowCalorieStreak(user, profile, today);

        // ─── FR-32.TriggerHighCalorie ─────────────────────────────────────
        created += checkHighCalorieStreak(user, profile, today);

        // ─── FR-32.TriggerBMI – backup scan ──────────────────────────────
        created += checkBmiAlert(user, profile);

        return created;
    }

    /**
     * FR-32.TriggerLowCalorie:
     * Nếu 3 ngày liên tiếp Calories_In < 50% TDEE → phát alert.
     */
    private int checkLowCalorieStreak(User user, HealthProfile profile, LocalDate today) {
        double tdee     = profile.getTdee();
        double minCal   = tdee * LOW_CALORIE_THRESHOLD_PCT;
        int    streak   = 0;

        for (int i = 0; i < CONSECUTIVE_DAYS_TRIGGER; i++) {
            LocalDate day = today.minusDays(i);
            double intake = mealRepository.sumCaloriesByUserAndDate(user.getId(), day);
            if (intake < minCal) streak++;
            else break;
        }

        if (streak >= CONSECUTIVE_DAYS_TRIGGER) {
            String alertType = "LOW_CALORIE_STREAK";
            if (hasRecentAlert(user.getId(), alertType)) return 0;

            String msg = String.format(
                "Ăn ít calo liên tục – nguy cơ thiếu dinh dưỡng: " +
                "Bạn đã ăn dưới %.0f kcal/ngày (50%% TDEE) trong %d ngày liên tiếp.",
                minCal, CONSECUTIVE_DAYS_TRIGGER);

            HealthAlert alert = createAndSaveAlert(user, alertType, "HIGH", msg,
                    (double) streak, (double) CONSECUTIVE_DAYS_TRIGGER);

            // FR-32.Notification – báo chuyên gia
            notifyExpert(user, alert);
            // FR-32.UserNotify – thông báo nhẹ đến người dùng
            notifyUser(user);

            return 1;
        }
        return 0;
    }

    /**
     * FR-32.TriggerHighCalorie:
     * Nếu 3 ngày liên tiếp Calories_In > 150% Daily_Calorie_Target → phát alert.
     */
    private int checkHighCalorieStreak(User user, HealthProfile profile, LocalDate today) {
        if (profile.getDailyCalorieGoal() == null) return 0;

        double dailyTarget = profile.getDailyCalorieGoal();
        double maxCal      = dailyTarget * HIGH_CALORIE_THRESHOLD_PCT;
        int    streak      = 0;

        for (int i = 0; i < CONSECUTIVE_DAYS_TRIGGER; i++) {
            LocalDate day = today.minusDays(i);
            double intake = mealRepository.sumCaloriesByUserAndDate(user.getId(), day);
            if (intake > maxCal) streak++;
            else break;
        }

        if (streak >= CONSECUTIVE_DAYS_TRIGGER) {
            String alertType = "HIGH_CALORIE_STREAK";
            if (hasRecentAlert(user.getId(), alertType)) return 0;

            String msg = String.format(
                "Ăn vượt calo liên tục – nguy cơ mất kiểm soát ăn uống: " +
                "Bạn đã nạp hơn %.0f kcal/ngày (150%% mục tiêu) trong %d ngày liên tiếp.",
                maxCal, CONSECUTIVE_DAYS_TRIGGER);

            HealthAlert alert = createAndSaveAlert(user, alertType, "MEDIUM", msg,
                    (double) streak, (double) CONSECUTIVE_DAYS_TRIGGER);

            notifyExpert(user, alert);
            notifyUser(user);
            return 1;
        }
        return 0;
    }

    /**
     * FR-32.TriggerBMI – backup scan.
     * Phát alert khi BMI ≤ 17.0 hoặc ≥ 30.0 (trường hợp chưa được trigger từ ProfileService).
     */
    private int checkBmiAlert(User user, HealthProfile profile) {
        if (profile.getBmi() == null) return 0;

        double bmi = profile.getBmi();
        String alertType = null;
        String message   = null;
        String severity  = "HIGH";

        if (bmi <= BMI_CRITICAL_LOW) {
            alertType = "BMI_CRITICAL_LOW";
            message   = String.format("Chỉ số BMI của bạn là %.1f (thiếu cân nghiêm trọng ≤ 17.0). " +
                        "Hãy tham khảo ý kiến chuyên gia dinh dưỡng ngay.", bmi);
        } else if (bmi >= BMI_OBESE) {
            alertType = "BMI_OBESE";
            message   = String.format("Chỉ số BMI của bạn là %.1f (béo phì ≥ 30.0). " +
                        "Hãy tham khảo ý kiến chuyên gia dinh dưỡng.", bmi);
        }

        if (alertType == null || hasRecentAlert(user.getId(), alertType)) return 0;

        HealthAlert alert = createAndSaveAlert(user, alertType, severity, message, bmi,
                bmi <= BMI_CRITICAL_LOW ? BMI_CRITICAL_LOW : BMI_OBESE);
        notifyExpert(user, alert);
        notifyUser(user);
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Cooldown 24h – tránh spam cùng loại alert (FR-32 implicit) */
    private boolean hasRecentAlert(Long userId, String alertType) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return !alertRepository.findByUserIdAndTypeAfter(userId, alertType, since).isEmpty();
    }

    private HealthAlert createAndSaveAlert(User user, String type, String severity,
                                            String message, double triggered, double threshold) {
        HealthAlert alert = HealthAlert.builder()
                .user(user)
                .alertType(type)
                .severity(severity)
                .message(message)
                .triggeredValue(triggered)
                .thresholdValue(threshold)
                .build();
        HealthAlert saved = alertRepository.save(alert);
        log.info("[RiskAlert] Created alert type={} severity={} for userId={}", type, severity, user.getId());
        return saved;
    }

    /**
     * FR-32.Notification – gửi email đến chuyên gia được gán.
     * Tìm tất cả chuyên gia active đang theo dõi user này.
     */
    private void notifyExpert(User user, HealthAlert alert) {
        try {
            List<?> assignments = expertAssignmentRepository
                    .findByUserIdAndStatus(user.getId(), "active");

            for (Object assignmentObj : assignments) {
                // Cast tới ExpertAssignment và lấy expert user
                if (assignmentObj instanceof com.vitatrack.entity.ExpertAssignment assignment) {
                    User expert = assignment.getExpert();
                    sendEmail(expert.getEmail(),
                        "[VitaTrack] Cảnh báo sức khỏe – " + user.getFullName(),
                        "Người dùng " + user.getFullName() + " có cảnh báo sức khỏe mới:\n\n"
                        + alert.getMessage()
                        + "\n\nVui lòng đăng nhập VitaTrack để xem chi tiết và liên hệ hỗ trợ người dùng.");
                }
            }
        } catch (Exception e) {
            log.warn("[RiskAlert] Expert notification failed for userId={}: {}", user.getId(), e.getMessage());
        }
    }

    /**
     * FR-32.UserNotify – thông báo nhẹ đến người dùng.
     * Không tiết lộ nội dung cụ thể của alert.
     */
    private void notifyUser(User user) {
        try {
            sendEmail(user.getEmail(),
                "[VitaTrack] Thông báo sức khỏe",
                "Chuyên gia của bạn đã được thông báo về chỉ số sức khỏe gần đây " +
                "và sẽ liên hệ hỗ trợ sớm.\n\n" +
                "Hãy đăng nhập VitaTrack để xem tình trạng sức khỏe của bạn.");
        } catch (Exception e) {
            log.warn("[RiskAlert] User notification failed for userId={}: {}", user.getId(), e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[RiskAlert] Email send failed to={}: {}", to, e.getMessage());
        }
    }
}
