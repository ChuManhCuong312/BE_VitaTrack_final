package com.vitatrack.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HealthProfileDTO – bổ sung theo SRS:
 *  • profileComplete + missingFields  FR-06.Incomplete (banner nhắc nhở)
 *  • bmiSafetyAlert                   FR-08.SafetyAlert (BMI < 17 hoặc >= 30)
 *  • goalProgress                     FR-07.GoalProgress (% tiến độ đạt mục tiêu)
 *  • goalAchieved                     FR-07.GoalAchieved (đã đạt target weight)
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class HealthProfileDTO {

    private Long    id;
    private Double  height;
    private Double  weight;
    private Integer age;
    private String  gender;
    private String  activityLevel;
    private String  goalType;
    private Double  targetWeight;
    private Integer dailyCalorieGoal;
    private Integer dailyStepsGoal;
    private Integer dailyWaterGoal;
    private List<String> allergies;

    // ── Computed indices (FR-08, FR-09) ──────────────────────────────────
    private Double bmi;
    private Double bmr;
    private Double tdee;
    /** "Thiếu cân" | "Bình thường" | "Thừa cân" | "Béo phì" */
    private String bmiCategory;

    // ── FR-06.Incomplete ──────────────────────────────────────────────────
    /** true nếu đã có đủ height, weight, age, gender, activityLevel */
    private boolean      profileComplete;
    /** Danh sách tên trường còn thiếu; rỗng nếu profileComplete=true */
    private List<String> missingFields;

    // ── FR-08.SafetyAlert ─────────────────────────────────────────────────
    /** true khi BMI < 17.0 hoặc BMI ≥ 30.0 – FE hiển thị banner cảnh báo */
    private boolean bmiSafetyAlert;

    // ── FR-07.GoalProgress / GoalAchieved ────────────────────────────────
    /**
     * Phần trăm tiến độ đạt mục tiêu cân nặng (0-100).
     * = |current - initial| / |initial - target| × 100.
     * null nếu chưa thiết lập mục tiêu.
     */
    private Double  goalProgressPct;
    /** true khi |current - target| ≤ 0.5 kg */
    private boolean goalAchieved;

    // ── Dashboard extras ──────────────────────────────────────────────────
    private List<WeightHistoryPoint> weightHistory;
    private Double  calorieIntakeToday;
    private Double  calorieBurnedToday;
    private Integer stepsToday;
    private Double  waterToday;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeightHistoryPoint {
        private LocalDateTime recordedAt;
        private Double        weight;
        private Double        bmi;
        private String        notes;
    }
}
