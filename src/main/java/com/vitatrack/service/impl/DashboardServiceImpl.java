package com.vitatrack.service.impl;

import com.vitatrack.entity.DailyNutritionLog;
import com.vitatrack.repository.ActivityRepository;
import com.vitatrack.repository.DailyNutritionLogRepository;
import com.vitatrack.repository.HealthProfileRepository;
import com.vitatrack.repository.MealRepository;
import com.vitatrack.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UC-13 – Dashboard tổng quan.
 *
 * Ưu tiên đọc calorie_budget từ DailyNutritionLog (đã được ActivityServiceImpl
 * tính và lưu sau mỗi sync hoạt động). Fallback tính thời gian thực nếu chưa có.
 * Đảm bảo nhất quán với FR-08 Dynamic Calorie Budget.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final HealthProfileRepository    healthProfileRepository;
    private final MealRepository             mealRepository;
    private final ActivityRepository         activityRepository;
    private final DailyNutritionLogRepository dailyLogRepository;

    @Override
    public Map<String, Object> getDashboard(Long userId) {
        LocalDate today = LocalDate.now();

        double caloriesIn  = mealRepository.sumCaloriesByUserAndDate(userId, today);
        double caloriesOut = activityRepository.sumCaloriesBurnedByUserAndDate(userId, today);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("date",          today.toString());
        dashboard.put("caloriesIn",    caloriesIn);
        dashboard.put("caloriesOut",   caloriesOut);
        dashboard.put("calorieBalance", caloriesIn - caloriesOut);

        healthProfileRepository.findByUserId(userId).ifPresent(p -> {
            dashboard.put("bmi",      p.getBmi());
            dashboard.put("bmr",      p.getBmr());
            dashboard.put("tdee",     p.getTdee());
            dashboard.put("weight",   p.getWeight());
            dashboard.put("goalType", p.getGoalType());

            int    baseGoal = p.getDailyCalorieGoal() != null ? p.getDailyCalorieGoal() : 2000;
            double tdee     = p.getTdee() != null ? p.getTdee() : baseGoal;

            dashboard.put("dailyCalorieGoal", baseGoal);

            // FR-08: Đọc dynamic budget từ DailyNutritionLog nếu đã được tính sẵn
            // (ActivityServiceImpl.recalcDynamicBudget lưu giá trị chính xác vào đây)
            DailyNutritionLog dnl = dailyLogRepository
                    .findByUserIdAndLogDate(userId, today)
                    .orElse(null);

            double dynamicBudget;
            double budgetBonus;

            if (dnl != null && dnl.getCalorieBudget() != null) {
                // Dùng giá trị đã lưu (tính sau sync wearable) – nhất quán tuyệt đối
                dynamicBudget = dnl.getCalorieBudget().doubleValue();
                // bonus = budget - baseGoal (không âm)
                budgetBonus = Math.max(0, dynamicBudget - baseGoal);
            } else {
                // Fallback: tính thời gian thực theo cùng công thức như ActivityServiceImpl
                double goalAdjustment = 0;
                if (p.getGoalType() != null) {
                    goalAdjustment = switch (p.getGoalType().toUpperCase()) {
                        case "LOSE_WEIGHT" -> -500;
                        case "GAIN_MUSCLE" -> +300;
                        default            -> 0;
                    };
                }
                double baselineBurn = tdee * 0.3;
                double rawBudget    = tdee + (caloriesOut - baselineBurn) + goalAdjustment;
                double minSafe      = "female".equalsIgnoreCase(p.getGender()) ? 1200 : 1500;
                dynamicBudget = Math.max(minSafe, Math.min(5000, rawBudget));
                budgetBonus   = Math.max(0, caloriesOut - baselineBurn);
            }

            dashboard.put("dynamicBudget", Math.round(dynamicBudget));
            dashboard.put("budgetBonus",   Math.round(budgetBonus));

            if (dynamicBudget > 0) {
                double pct = caloriesIn / dynamicBudget * 100;
                dashboard.put("calorieProgressPercent", Math.min(Math.round(pct * 10.0) / 10.0, 100.0));
                dashboard.put("calorieOverage", caloriesIn > dynamicBudget * 1.2);
            }

            // Macro tổng ngày (từ DailyNutritionLog nếu có)
            if (dnl != null) {
                if (dnl.getTotalProteinG() != null) dashboard.put("totalProtein", dnl.getTotalProteinG());
                if (dnl.getTotalCarbsG()   != null) dashboard.put("totalCarbs",   dnl.getTotalCarbsG());
                if (dnl.getTotalFatG()     != null) dashboard.put("totalFat",     dnl.getTotalFatG());
                if (dnl.getCalorieBalance() != null) dashboard.put("calorieBalance", dnl.getCalorieBalance());
            }
        });

        return dashboard;
    }
}
