package com.vitatrack.utils;

import org.springframework.stereotype.Component;

@Component
public class BMIUtil {

    /**
     * Calculate BMI from weight (kg) and height (cm).
     */
    public double calculateBMI(double weightKg, double heightCm) {
        double heightM = heightCm / 100.0;
        return Math.round((weightKg / (heightM * heightM)) * 10.0) / 10.0;
    }

    /**
     * Mifflin-St Jeor Equation for BMR.
     * gender: "male" | "female"
     */
    public double calculateBMR(double weightKg, double heightCm, int age, String gender) {
        double bmr = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        if ("male".equalsIgnoreCase(gender)) {
            bmr += 5;
        } else {
            bmr -= 161;
        }
        return Math.round(bmr);
    }

    /**
     * Harris-Benedict activity multipliers for TDEE.
     */
    public double calculateTDEE(double bmr, String activityLevel) {
        double factor = switch (activityLevel == null ? "sedentary" : activityLevel.toLowerCase()) {
            case "light"       -> 1.375;
            case "moderate"    -> 1.55;
            case "active"      -> 1.725;
            case "very_active" -> 1.9;
            default            -> 1.2;  // sedentary
        };
        return Math.round(bmr * factor);
    }

    /**
     * Daily calorie goal based on TDEE and goal type.
     * lose_weight: -500 kcal/day | gain_weight: +500 kcal/day | maintain: 0
     */
    public int calculateDailyCalorieGoal(double tdee, String goalType) {
        int adjustment = switch (goalType == null ? "maintain" : goalType.toLowerCase()) {
            case "lose_weight" -> -500;
            case "gain_weight" -> +500;
            default            -> 0;
        };
        return (int) Math.max(1200, tdee + adjustment);  // floor at 1200 kcal for safety
    }

    public String getBMICategory(double bmi) {
        if (bmi < 18.5)  return "Thiếu cân";
        if (bmi < 25.0)  return "Bình thường";
        if (bmi < 30.0)  return "Thừa cân";
        return "Béo phì";
    }

    public boolean isBMIAtRisk(double bmi) {
        return bmi < 18.5 || bmi >= 30.0;
    }
}
