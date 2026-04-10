package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * WeightHistory – bổ sung theo SRS FR-10:
 *  • notes (FR-10.AutoSave / ManualRecord – ghi chú tùy chọn tối đa 100 ký tự)
 *  • source ("profile_update" | "manual" – FR-10.AutoSave)
 */
@Entity
@Table(name = "weight_history", indexes = {
    @Index(name = "IX_WeightHistory_ProfileId", columnList = "health_profile_id"),
    @Index(name = "IX_WeightHistory_RecordedAt", columnList = "recorded_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WeightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_profile_id", nullable = false)
    private HealthProfile healthProfile;

    /** Cân nặng tại thời điểm ghi (kg) */
    @Column(nullable = false)
    private Double weight;

    /** BMI tại thời điểm ghi (tự động tính nếu có height) */
    @Column
    private Double bmi;

    /**
     * FR-10.AutoSave    → "profile_update"
     * FR-10.ManualRecord → "manual"
     */
    @Column(length = 30)
    @Builder.Default
    private String source = "profile_update";

    /** FR-10.ManualRecord / FR-10.AutoSave – ghi chú tùy chọn, tối đa 100 ký tự */
    @Column(length = 100)
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }
}
