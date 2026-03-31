package com.vitatrack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Meal plan created by an expert and sent to a client.
 * status: "draft" | "sent"
 */
@Entity
@Table(name = "meal_plans", indexes = {
    @Index(name = "IX_MealPlans_Expert", columnList = "expert_id"),
    @Index(name = "IX_MealPlans_Client", columnList = "client_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(nullable = false, length = 200)
    private String title;

    /** Duration in days */
    @Column
    private Integer duration;

    @Column(name = "target_calories")
    private Integer targetCalories;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** draft | sent */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
