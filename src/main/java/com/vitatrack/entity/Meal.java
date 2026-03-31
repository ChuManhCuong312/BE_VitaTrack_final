package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Legacy Meal entity - kept for DB schema compatibility.
 * New code uses MealLog for flat food-diary entries.
 */
@Entity
@Table(name = "meals", indexes = {
    @Index(name = "IX_Meals_UserDate", columnList = "user_id, log_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "meal_name", length = 200)
    private String mealName;

    @Column(name = "total_calories")
    private Double totalCalories;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    private List<MealItem> items;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
