package com.vitatrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity – bổ sung theo SRS FR-01 → FR-05, FR-35:
 *
 *  • isEmailVerified + emailVerificationToken/Expiry     FR-01 Email Verification
 *  • verificationResendCount + verificationResendWindowStart  FR-01.ResendLimit
 *  • loginAttempts + loginLockedUntil                    FR-02.RateLimit
 *  • otpCode + otpExpiry + otpAttempts + otpSendCount    FR-04 OTP reset
 *  • lastLoginAt                                         FR-35 Admin view
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "IX_Users_Email",       columnList = "email"),
    @Index(name = "IX_Users_VerifToken",  columnList = "email_verification_token"),
    @Index(name = "IX_Users_OtpCode",     columnList = "otp_code")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Email Verification ─────────────────────────────────────────────────
    /** FR-02.UnverifiedAccount – Google SSO = true ngay, email/pw = false cho đến khi xác thực */
    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private boolean isEmailVerified = false;

    @JsonIgnore
    @Column(name = "email_verification_token", length = 64)
    private String emailVerificationToken;

    @JsonIgnore
    @Column(name = "email_verification_expiry")
    private LocalDateTime emailVerificationExpiry;

    /** FR-01.ResendLimit – đếm số lần gửi lại trong cửa sổ 1 giờ */
    @Column(name = "verification_resend_count", nullable = false)
    @Builder.Default
    private int verificationResendCount = 0;

    @Column(name = "verification_resend_window_start")
    private LocalDateTime verificationResendWindowStart;

    // ── Login Rate Limiting ────────────────────────────────────────────────
    /** FR-02.RateLimit – 5 lần sai liên tiếp → khoá 15 phút */
    @Column(name = "login_attempts", nullable = false)
    @Builder.Default
    private int loginAttempts = 0;

    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    // ── OTP Password Reset ────────────────────────────────────────────────
    /** FR-04 – mã OTP 6 số (lưu plain để tiện query; nên hash SHA-256 trên production) */
    @JsonIgnore
    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @JsonIgnore
    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    /** FR-04.Step2.WrongOTP – tối đa 3 lần sai */
    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private int otpAttempts = 0;

    /** FR-04.OTPRules – tối đa 3 lần gửi trong 1 giờ */
    @Column(name = "otp_send_count", nullable = false)
    @Builder.Default
    private int otpSendCount = 0;

    @Column(name = "otp_window_start")
    private LocalDateTime otpWindowStart;

    // ── Legacy reset token (giữ tương thích) ─────────────────────────────
    @JsonIgnore
    @Column(name = "reset_token")
    private String resetToken;

    @JsonIgnore
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // ── Metadata ──────────────────────────────────────────────────────────
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "expert_status", length = 20)
    private String expertStatus;

    @Column(name = "expert_reject_reason")
    private String expertRejectReason;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Relationships ─────────────────────────────────────────────────────
    @JsonIgnore
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private HealthProfile healthProfile;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MealLog> mealLogs;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActivityLog> activityLogs;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Goal> goals;

    // ── Lifecycle ─────────────────────────────────────────────────────────
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ── UserDetails ───────────────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String  getPassword()             { return passwordHash; }
    @Override public String  getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return isActive; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return isActive; }

    // ── Helpers ───────────────────────────────────────────────────────────
    /** FR-02.RateLimit – tài khoản có đang bị khoá tạm thời? */
    public boolean isTemporarilyLocked() {
        return loginLockedUntil != null && loginLockedUntil.isAfter(LocalDateTime.now());
    }

    /** FR-04.OTPRules – OTP còn hiệu lực và chưa vượt số lần thử */
    public boolean isOtpValid() {
        return otpCode != null
            && otpExpiry != null
            && otpExpiry.isAfter(LocalDateTime.now())
            && otpAttempts < 3;
    }
}
