package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "allergies",
    uniqueConstraints = @UniqueConstraint(name = "UQ_Allergies_UserAllergen",
        columnNames = {"user_id", "allergen_name"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Gluten | Lactose | Nut | Shellfish | ... */
    @Column(name = "allergen_name", nullable = false, length = 100)
    private String allergenName;

    /** LOW | MEDIUM | HIGH */
    @Column(length = 20)
    private String severity;

    @Column(name = "noted_at")
    private LocalDateTime notedAt;

    @PrePersist
    protected void onCreate() { if (notedAt == null) notedAt = LocalDateTime.now(); }
}
