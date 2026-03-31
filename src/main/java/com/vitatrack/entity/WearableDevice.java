package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wearable_devices")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WearableDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FITBIT | GARMIN | APPLE_WATCH | SAMSUNG | GENERIC */
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "external_device_id", nullable = false, length = 200)
    private String externalDeviceId;

    /** OAuth2 Access Token – AES-256 encrypted at rest */
    @JsonIgnore
    @Column(name = "access_token", length = 500)
    private String accessToken;

    /** OAuth2 Refresh Token – AES-256 encrypted at rest */
    @JsonIgnore
    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
