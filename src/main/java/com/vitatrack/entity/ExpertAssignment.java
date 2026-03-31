package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Links an expert to a client they are responsible for.
 *
 * Bổ sung:
 *  • status ("active" | "ended")   FR-35.AssignExpert / FR-35.RevokeExpert
 *  • endedAt                        Ghi nhận thời điểm hủy phân công
 */
@Entity
@Table(name = "expert_assignments", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_ExpertAssignment", columnNames = {"expert_id", "client_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpertAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expert_id", nullable = false)
    private User expert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    /**
     * FR-35.AssignExpert  → "active"
     * FR-35.RevokeExpert  → "ended"
     * FR-30.AccessControl – chỉ được xem khi status = "active"
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    /** Thời điểm hủy phân công (FR-35.RevokeExpert) */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    protected void onCreate() { assignedAt = LocalDateTime.now(); }
}
