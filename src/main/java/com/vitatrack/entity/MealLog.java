package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Flat meal-log entry (one food item per row).
 * food is EAGER so MealServiceImpl can read nutritional data in the same transaction.
 * user is LAZY + @JsonIgnore to prevent serialization issues.
 */
@Entity
@Table(name = "meal_logs", indexes = {
    @Index(name = "IX_MealLogs_UserDate", columnList = "user_id, log_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** EAGER: needed to read nutritional info during meal response mapping */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    /** BREAKFAST | LUNCH | DINNER | SNACK */
    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double quantity;

    /** g | ml | serving */
    @Column(length = 20)
    private String unit;

    @Column
    private Double calories;

    @Column
    private Double protein;

    @Column
    private Double carbs;

    @Column
    private Double fat;

    @Column(name = "added_via_ai")
    private boolean addedViaAi;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
