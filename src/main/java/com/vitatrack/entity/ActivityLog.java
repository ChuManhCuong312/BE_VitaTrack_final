package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "IX_ActivityLogs_UserDate", columnList = "user_id, log_date")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** walking | running | cycling | swimming | gym | yoga | other */
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    /** Duration in minutes */
    @Column(nullable = false)
    private Integer duration;

    @Column(name = "calories_burned")
    private Double caloriesBurned;

    @Column
    private Double distance;

    @Column(name = "steps_count")
    private Integer stepsCount;

    /** Số giờ ngủ – SLEEP_DEFICIT alert khi < 6h (FR-09) */
    @Column(name = "sleep_hours")
    private Double sleepHours;

    /** Nhịp tim TB (bpm) – TACHYCARDIA (>100) / BRADYCARDIA (<50) (FR-09) */
    @Column(name = "heart_rate_avg")
    private Integer heartRateAvg;

    @Column(name = "heart_rate_max")
    private Integer heartRateMax;

    @Column(name = "log_date", nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String notes;

    /** manual | fitbit | garmin | google_fit | apple_health */
    @Column(length = 30)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
