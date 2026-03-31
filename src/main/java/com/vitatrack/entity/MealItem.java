package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "meal_items", indexes = {
    @Index(name = "IX_MealItems_MealId", columnList = "meal_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    /** grams consumed */
    @Column(name = "quantity_g", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantityG;

    /** (quantity_g / 100) × calories_per_100g */
    @Column(name = "calories_consumed", precision = 8, scale = 2)
    private BigDecimal caloriesConsumed;
}
