package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * HealthAlert – bổ sung theo FR-32.Acknowledge:
 *  • acknowledged      true khi chuyên gia đánh dấu đã xử lý
 *  • acknowledgedNote  Ghi chú bắt buộc 10-200 ký tự (FR-32.Acknowledge)
 *  • acknowledgedAt    Thời điểm xử lý
 */
@Entity
@Table(name = "health_alerts", indexes = {
    @Index(name = "IX_HealthAlerts_UserId",    columnList = "user_id"),
    @Index(name = "IX_HealthAlerts_AlertType", columnList = "alert_type"),
    @Index(name = "IX_HealthAlerts_CreatedAt", columnList = "created_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HealthAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Loại cảnh báo – đồng nhất với AlertServiceImpl:
     * BMI_UNDERWEIGHT | BMI_OVERWEIGHT | BMI_OBESE | BMI_CRITICAL_LOW |
     * CALORIE_OVERAGE | CALORIE_DEFICIT | NO_LOG_TODAY |
     * LOW_PROTEIN | LOW_CALORIE_STREAK | HIGH_CALORIE_STREAK |
     * SLEEP_DEFICIT | TACHYCARDIA | BRADYCARDIA
     */
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    @Column(nullable = false, length = 10)
    private String severity;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "triggered_value")
    private Double triggeredValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    /** FR-32.AlertDisplay – user đã đọc thông báo chưa */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    // ── FR-32.Acknowledge ─────────────────────────────────────────────────

    /** Expert đã đánh dấu xử lý */
    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private boolean acknowledged = false;

    /** Bắt buộc 10-200 ký tự khi acknowledge (FR-32.Acknowledge) */
    @Column(name = "acknowledged_note", length = 200)
    private String acknowledgedNote;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
