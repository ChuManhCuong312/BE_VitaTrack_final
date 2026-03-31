package com.vitatrack.service.impl;

import com.vitatrack.dto.activity.ActivityDTO;
import com.vitatrack.dto.activity.ActivityResponse;
import com.vitatrack.entity.ActivityLog;
import com.vitatrack.entity.DailyNutritionLog;
import com.vitatrack.entity.HealthProfile;
import com.vitatrack.entity.User;
import com.vitatrack.repository.ActivityRepository;
import com.vitatrack.repository.DailyNutritionLogRepository;
import com.vitatrack.repository.HealthProfileRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.ActivityService;
import com.vitatrack.utils.CalorieUtil;
import com.vitatrack.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final DailyNutritionLogRepository dailyLogRepository;
    private final CalorieUtil calorieUtil;
    private final DateUtil dateUtil;

    // ── getToday ──────────────────────────────────────────────────────────

    @Override
    public List<ActivityResponse> getToday(Long userId) {
        return activityRepository.findByUserIdAndDateOrderByCreatedAtAsc(userId, LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── addActivity ───────────────────────────────────────────────────────

    @Override
    public ActivityResponse addActivity(Long userId, ActivityDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.vitatrack.exception.VitaTrackException.ResourceNotFoundException("Người dùng"));

        // FR-22.Field.ActivityDate – không được chọn ngày tương lai
        LocalDate logDate = dto.getDate() != null ? dto.getDate() : LocalDate.now();
        if (logDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể ghi nhận hoạt động cho ngày trong tương lai.");
        }

        // FR-22.InvalidDuration – thời lượng phải từ 1 đến 720 phút
        int duration = dto.getDuration() != null ? dto.getDuration() : 0;
        if (duration < 1 || duration > 720) {
            throw new com.vitatrack.exception.VitaTrackException.InvalidDurationException();
        }

        // FR-22.Field.CalcCalories – nếu không truyền calories thì ước tính từ MET
        // FR-22.MissingWeight – nếu chưa có cân nặng thì lưu calories = 0
        double caloriesBurned;
        if (dto.getCalories() != null) {
            caloriesBurned = dto.getCalories();
        } else {
            boolean hasWeight = healthProfileRepository.findByUserId(userId)
                    .map(hp -> hp.getWeight() != null).orElse(false);
            caloriesBurned = hasWeight
                    ? estimateCalories(userId, dto.getType(), duration)
                    : 0.0;
        }

        ActivityLog log = ActivityLog.builder()
                .user(user)
                .activityType(dto.getType())
                .duration(duration)
                .caloriesBurned(caloriesBurned)
                .distance(dto.getDistance())
                .stepsCount(dto.getStepsCount())
                .sleepHours(dto.getSleepHours())
                .heartRateAvg(dto.getHeartRateAvg())
                .heartRateMax(dto.getHeartRateMax())
                .date(logDate)
                .notes(dto.getNotes())
                .source("manual")
                .build();

        ActivityLog saved = activityRepository.save(log);
        recalcDynamicBudget(userId, logDate);
        return toResponse(saved);
    }

    // ── deleteActivity ────────────────────────────────────────────────────

    @Override
    public void deleteActivity(Long userId, Long activityId) {
        ActivityLog act = activityRepository.findById(activityId)
                .orElseThrow(() -> new com.vitatrack.exception.VitaTrackException.ResourceNotFoundException("Hoạt động"));
        if (!act.getUser().getId().equals(userId))
            throw new com.vitatrack.exception.VitaTrackException.ExpertAccessDeniedException();

        LocalDate logDate = act.getDate();
        activityRepository.deleteById(activityId);
        recalcDynamicBudget(userId, logDate);
    }

    // ── getHistory ────────────────────────────────────────────────────────

    @Override
    public List<ActivityResponse> getHistory(Long userId, String from, String to) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now().minusDays(30);
        LocalDate toDate   = to   != null ? LocalDate.parse(to)   : LocalDate.now();
        return activityRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, fromDate, toDate)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── syncWearable ──────────────────────────────────────────────────────

    @Override
    public Map<String, Object> syncWearable(Long userId, String device, String accessToken) {
        // Stub: production sẽ gọi Fitbit/Garmin API với accessToken
        log.info("Wearable sync requested: userId={}, device={}", userId, device);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("device", device);
        result.put("message", "Đồng bộ dữ liệu thành công (demo).");
        result.put("syncedDate", LocalDate.now().toString());
        result.put("steps", 0);
        result.put("caloriesBurned", 0);
        return result;
    }

    // ── getWearableStatus ─────────────────────────────────────────────────

    @Override
    public Map<String, Object> getWearableStatus(Long userId) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("connected", false);
        status.put("device", null);
        status.put("lastSync", null);
        return status;
    }

    // ── getStats ──────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getStats(Long userId, String period) {
        LocalDate from = dateUtil.periodToStartDate(period);
        LocalDate to   = LocalDate.now();

        List<Object[]> rows = activityRepository.getDailyActivityStats(userId, from, to);
        List<Map<String, Object>> daily = rows.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date",          r[0].toString());
            m.put("caloriesBurned", r[1]);
            m.put("steps",         r[2]);
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", period);
        result.put("from",   from.toString());
        result.put("to",     to.toString());
        result.put("daily",  daily);
        return result;
    }

    // ── FR-08: Dynamic Calorie Budget Calculation ─────────────────────────
    /**
     * Budget = TDEE + (calories_burned_today - baseline_burn) +/- goal_adjustment
     * Clamp: min [1200 (female) / 1500 (male)], max 5000
     *
     * Cập nhật vào DailyNutritionLog.calorie_budget và tính lại calorie_balance.
     */
    public void recalcDynamicBudget(Long userId, LocalDate date) {
        try {
            HealthProfile hp = healthProfileRepository.findByUserId(userId).orElse(null);
            if (hp == null || hp.getTdee() == null) return;

            double tdee            = hp.getTdee();
            double actualBurned    = activityRepository.sumCaloriesBurnedByUserAndDate(userId, date);
            // Baseline = TDEE / 24h * fraction. Đơn giản hoá: baseline = tdee * 0.3 (30% từ vận động chủ động)
            double baselineBurn    = tdee * 0.3;
            double goalAdjustment  = 0;

            if (hp.getGoalType() != null) {
                goalAdjustment = switch (hp.getGoalType().toUpperCase()) {
                    case "LOSE_WEIGHT" -> -500;
                    case "GAIN_MUSCLE" -> +300;
                    default             -> 0;
                };
            }

            double rawBudget = tdee + (actualBurned - baselineBurn) + goalAdjustment;

            // Clamp theo giới tính
            double minSafe = "female".equalsIgnoreCase(hp.getGender()) ? 1200 : 1500;
            double budget  = Math.max(minSafe, Math.min(5000, rawBudget));

            User user = userRepository.findById(userId).orElseThrow();
            DailyNutritionLog dnl = dailyLogRepository
                    .findByUserIdAndLogDate(userId, date)
                    .orElseGet(() -> DailyNutritionLog.builder()
                            .user(user).logDate(date).build());

            BigDecimal budgetBD  = BigDecimal.valueOf(Math.round(budget));
            BigDecimal totalCal  = dnl.getTotalCalories() != null ? dnl.getTotalCalories() : BigDecimal.ZERO;
            BigDecimal balanceBD = budgetBD.subtract(totalCal);

            dnl.setCalorieBudget(budgetBD);
            dnl.setCalorieBalance(balanceBD);
            dailyLogRepository.save(dnl);

            log.debug("[DynamicBudget] userId={} date={} tdee={} burned={} budget={} balance={}",
                    userId, date, tdee, actualBurned, budget, balanceBD);

        } catch (Exception e) {
            log.warn("[DynamicBudget] Failed for userId={}: {}", userId, e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private double estimateCalories(Long userId, String type, int durationMinutes) {
        double weight = healthProfileRepository.findByUserId(userId)
                .map(hp -> hp.getWeight() != null ? hp.getWeight() : 70.0)
                .orElse(70.0);
        return calorieUtil.estimateCaloriesBurned(type, durationMinutes, weight);
    }

    private ActivityResponse toResponse(ActivityLog a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .activityType(a.getActivityType())
                .duration(a.getDuration())
                .caloriesBurned(a.getCaloriesBurned())
                .distance(a.getDistance())
                .stepsCount(a.getStepsCount())
                .sleepHours(a.getSleepHours())
                .heartRateAvg(a.getHeartRateAvg())
                .heartRateMax(a.getHeartRateMax())
                .date(a.getDate())
                .notes(a.getNotes())
                .source(a.getSource())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
