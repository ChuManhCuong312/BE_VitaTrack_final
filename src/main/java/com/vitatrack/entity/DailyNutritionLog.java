package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_nutrition_logs",
    uniqueConstraints = @UniqueConstraint(name = "UQ_DailyLog_UserDate",
        columnNames = {"user_id", "log_date"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DailyNutritionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "total_calories", precision = 8, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "total_protein_g", precision = 8, scale = 2)
    private BigDecimal totalProteinG;

    @Column(name = "total_fat_g", precision = 8, scale = 2)
    private BigDecimal totalFatG;

    @Column(name = "total_carbs_g", precision = 8, scale = 2)
    private BigDecimal totalCarbsG;

    /** TDEE ± điều chỉnh dựa trên vận động trong ngày (FR-08) */
    @Column(name = "calorie_budget", precision = 8, scale = 2)
    private BigDecimal calorieBudget;

    /** calorie_budget − total_calories */
    @Column(name = "calorie_balance", precision = 8, scale = 2)
    private BigDecimal calorieBalance;
}
