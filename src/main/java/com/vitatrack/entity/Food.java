package com.vitatrack.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Food entity – bổ sung theo SRS FR-33:
 *  • isActive (FR-33.SoftDelete) – Inactive: ẩn khỏi tìm kiếm, không xóa DB
 *  • fiberPer100g (FR-33.Field.Fiber)
 *  • servingUnit (FR-33.Field.ServingUnit)
 */
@Entity
@Table(name = "foods", indexes = {
    @Index(name = "IX_Foods_Name",     columnList = "name"),
    @Index(name = "IX_Foods_Category", columnList = "category"),
    @Index(name = "IX_Foods_IsActive", columnList = "is_active")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Food {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "calories_per_100g", nullable = false)
    private Double caloriesPer100g;

    @Column(name = "protein_per_100g")
    private Double proteinPer100g;

    @Column(name = "carbs_per_100g")
    private Double carbsPer100g;

    @Column(name = "fat_per_100g")
    private Double fatPer100g;

    /** FR-33.Field.Fiber */
    @Column(name = "fiber_per_100g")
    private Double fiberPer100g;

    @Column(length = 100)
    private String category;

    /** FR-33.Field.ServingUnit – e.g. "1 chén = 200g" */
    @Column(name = "serving_unit", length = 100)
    private String servingUnit;

    /** Pipe-separated allergen tags: "Đậu phộng|Gluten" */
    @Column(length = 500)
    private String allergens;

    @Column(name = "created_by_admin")
    @Builder.Default
    private boolean createdByAdmin = false;

    /**
     * FR-33.SoftDelete – true = hiển thị trong tìm kiếm; false = ẩn.
     * Không bao giờ xóa bản ghi để bảo toàn lịch sử MealItem.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
