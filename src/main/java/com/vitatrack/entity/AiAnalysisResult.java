package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AiAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** JSON array: [{food_name, confidence, estimated_weight_g, food_id}] */
    @Column(name = "detected_foods", columnDefinition = "NVARCHAR(MAX)")
    private String detectedFoods;

    @Column(name = "total_estimated_calories")
    private Double totalEstimatedCalories;

    @Column(name = "raw_api_response", columnDefinition = "NVARCHAR(MAX)")
    private String rawApiResponse;

    /** PENDING | PROCESSING | COMPLETED | FAILED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
