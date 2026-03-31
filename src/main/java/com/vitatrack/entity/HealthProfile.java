package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_profiles", indexes = {
    @Index(name = "IX_HealthProfiles_UserId", columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HealthProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** cm */
    @Column(name = "height_cm")
    private Double height;

    /** kg */
    @Column(name = "weight_kg")
    private Double weight;

    /** MALE | FEMALE | OTHER */
    @Column(length = 10)
    private String gender;

    /** Age in years */
    @Column
    private Integer age;

    /** SEDENTARY | LIGHT | MODERATE | ACTIVE | VERY_ACTIVE */
    @Column(name = "activity_level", length = 30)
    private String activityLevel;

    /** LOSE_WEIGHT | MAINTAIN | GAIN_WEIGHT */
    @Column(name = "goal_type", length = 30)
    private String goalType;

    @Column(name = "target_weight_kg")
    private Double targetWeight;

    @Column
    private Double bmi;

    @Column
    private Double bmr;

    @Column
    private Double tdee;

    @Column(name = "daily_calorie_goal")
    private Integer dailyCalorieGoal;

    @Column(name = "daily_steps_goal")
    private Integer dailyStepsGoal;

    @Column(name = "daily_water_goal")
    private Integer dailyWaterGoal;

    /** JSON array of allergy strings */
    @Column(name = "allergies", length = 1000)
    private String allergies;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
