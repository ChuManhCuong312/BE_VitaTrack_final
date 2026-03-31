package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "goals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** LOSE_WEIGHT | MAINTAIN | GAIN_MUSCLE */
    @Column(name = "goal_type", nullable = false, length = 30)
    private String goalType;

    @Column(name = "target_value")
    private Double targetValue;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** ACTIVE | COMPLETED | CANCELLED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
