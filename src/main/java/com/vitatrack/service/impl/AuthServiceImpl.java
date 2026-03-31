package com.vitatrack.service.impl;

import com.vitatrack.dto.auth.AuthResponse;
import com.vitatrack.dto.auth.LoginRequest;
import com.vitatrack.dto.auth.RegisterRequest;
import com.vitatrack.entity.RefreshToken;
import com.vitatrack.entity.Role;
import com.vitatrack.entity.User;
import com.vitatrack.exception.VitaTrackException;
import com.vitatrack.repository.RefreshTokenRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.security.JwtService;
import com.vitatrack.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * AuthServiceImpl – đầy đủ theo SRS FR-01 → FR-05:
 *
 *  FR-01  Register with email verification (isEmailVerified = false, gửi link 24h)
 *  FR-01.ResendLimit  Tối đa 3 lần gửi lại trong 1 giờ
 *  FR-01.ExpiredLink  Kiểm tra token hết hạn, trả 400 có nút gửi lại
 *  FR-02  Login – kiểm tra emailVerified, khoá tạm thời sau 5 sai, khoá Admin
 *  FR-02.RateLimit    5 lần sai → khoá 15 phút; reset khi đăng nhập thành công
 *  FR-03  Google SSO  – isEmailVerified = true tự động
 *  FR-04  OTP 6 số reset mật khẩu (không dùng link UUID)
 *  FR-04.OTPRules     10 phút, 1 lần dùng, max 3 lần sai, max 3 gửi/giờ
 *  FR-05  Logout + LogoutAll
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_ATTEMPTS    = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final int VERIFICATION_LINK_HOURS = 24;
    private static final int MAX_RESEND_PER_HOUR   = 3;
    private static final int OTP_VALID_MINUTES     = 10;
    private static final int MAX_OTP_ATTEMPTS      = 3;
    private static final int MAX_OTP_SEND_PER_HOUR = 3;

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;
    private final JwtService             jwtService;
    private final JavaMailSender         mailSender;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // ═══════════════════════════════════════════════════════════════
    // FR-01  Register
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new VitaTrackException.EmailAlreadyExistsException();
        }

        // Server-side password validation (FR-01.ValidatePassword)
        validatePasswordStrength(request.getPassword());

        String verToken = UUID.randomUUID().toString().replace("-", "");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.USER)
                .isActive(true)
                .isEmailVerified(false)                             // FR-01 – cần xác thực
                .emailVerificationToken(verToken)
                .emailVerificationExpiry(LocalDateTime.now().plusHours(VERIFICATION_LINK_HOURS))
                .verificationResendCount(0)
                .loginAttempts(0)
                .otpSendCount(0)
                .otpAttempts(0)
                .build();

        userRepository.save(user);

        sendVerificationEmail(user.getEmail(), verToken);
        log.info("[Auth] New user registered: email={}", user.getEmail());
        // Response: "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."
    }

    // ── FR-01.EmailVerification ────────────────────────────────────────────

    @Override
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(VitaTrackException.VerificationLinkExpiredException::new);

        if (user.getEmailVerificationExpiry() == null
                || user.getEmailVerificationExpiry().isBefore(LocalDateTime.now())) {
            throw new VitaTrackException.VerificationLinkExpiredException();
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiry(null);
        userRepository.save(user);
        // Redirect FE → trang đăng nhập + "Tài khoản đã được kích hoạt thành công."
    }

    // ── FR-01.ResendLimit ──────────────────────────────────────────────────

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.isEmailVerified()) return; // silent – không rò rỉ

        checkAndIncrementResendCount(user);   // throws ResendLimitExceededException nếu > 3

        String newToken = UUID.randomUUID().toString().replace("-", "");
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationExpiry(LocalDateTime.now().plusHours(VERIFICATION_LINK_HOURS));
        userRepository.save(user);

        sendVerificationEmail(email, newToken);
    }

    // ═══════════════════════════════════════════════════════════════
    // FR-02  Login
    // ═══════════════════════════════════════════════════════════════

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // FR-02.WrongCredentials – không tiết lộ trường nào sai
        if (user == null) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng.");
        }

        // FR-02.LockedAccount – Admin khoá vĩnh viễn
        if (!user.isActive()) {
            throw new VitaTrackException.AccountPermanentlyLockedException();
        }

        // FR-02.RateLimit – khoá tạm thời
        if (user.isTemporarilyLocked()) {
            throw new VitaTrackException.AccountTemporarilyLockedException();
        }

        // FR-02.UnverifiedAccount
        if (!user.isEmailVerified()) {
            throw new VitaTrackException.EmailNotVerifiedException();
        }

        // Xác thực mật khẩu
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            incrementLoginAttempts(user);   // có thể khoá tạm thời sau 5 lần
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng.");
        }

        // Đăng nhập thành công → reset bộ đếm
        user.setLoginAttempts(0);
        user.setLoginLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user, request.isRememberMe());
    }

    // ═══════════════════════════════════════════════════════════════
    // FR-03  Google SSO
    // ═══════════════════════════════════════════════════════════════

    @Override
    public AuthResponse loginOrRegisterWithGoogle(String email, String fullName, String avatarUrl) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // FR-03.NewUser – tự tạo tài khoản, isEmailVerified = true
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // unusable
                    .fullName(fullName != null ? fullName : email.split("@")[0])
                    .avatarUrl(avatarUrl)
                    .role(Role.USER)
                    .isActive(true)
                    .isEmailVerified(true)   // Google đã xác thực
                    .loginAttempts(0)
                    .otpSendCount(0)
                    .otpAttempts(0)
                    .build();
            return userRepository.save(newUser);
        });

        // FR-03.ExistingUser – liên kết SSO, cho đăng nhập bình thường
        if (!user.isActive()) {
            throw new VitaTrackException.AccountPermanentlyLockedException();
        }

        user.setLastLoginAt(LocalDateTime.now());
        if (avatarUrl != null && user.getAvatarUrl() == null) user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return buildAuthResponse(user, false);
    }

    // ═══════════════════════════════════════════════════════════════
    // FR-04  Forgot / Reset Password via OTP
    // ═══════════════════════════════════════════════════════════════

    /** FR-04.Step1 – Gửi OTP 6 số; luôn trả 200 dù email không tồn tại */
    @Override
    public void sendOtp(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            checkAndIncrementOtpSendCount(user);   // throws ResendLimitExceededException nếu > 3/giờ

            String otp = String.format("%06d", new Random().nextInt(1_000_000));
            user.setOtpCode(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
            user.setOtpAttempts(0);
            userRepository.save(user);

            sendOtpEmail(email, otp);
            log.info("[Auth] OTP sent to email={}", email);
        });
        // FR-04.Step1.Response – luôn trả thông báo chung, dù email có tồn tại hay không
    }

    /** FR-04.Step2 – Xác nhận OTP */
    @Override
    public void verifyOtp(String email, String inputOtp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VitaTrackException.InvalidOtpException(0));

        if (user.getOtpCode() == null || user.getOtpExpiry() == null) {
            throw new VitaTrackException.InvalidOtpException(0);
        }

        // FR-04.Step2.Expired
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new VitaTrackException.OtpExpiredException();
        }

        // FR-04.Step2.WrongOTP
        if (!user.getOtpCode().equals(inputOtp)) {
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);
            if (attempts >= MAX_OTP_ATTEMPTS) {
                // Vô hiệu hoá OTP ngay
                user.setOtpCode(null);
                user.setOtpExpiry(null);
                userRepository.save(user);
                throw new VitaTrackException.InvalidOtpException(0);
            }
            userRepository.save(user);
            throw new VitaTrackException.InvalidOtpException(MAX_OTP_ATTEMPTS - attempts);
        }

        // OTP hợp lệ – đánh dấu đã dùng (chưa xoá, xoá sau khi reset xong)
        user.setOtpAttempts(0);
        userRepository.save(user);
    }

    /** FR-04.Step3 – Đặt lại mật khẩu mới */
    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new VitaTrackException.InvalidOtpException(0));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)
                || user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new VitaTrackException.InvalidOtpException(0);
        }

        validatePasswordStrength(newPassword);

        // FR-04.Step3.Submit – cập nhật mật khẩu, vô hiệu token
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);

        // Vô hiệu hoá toàn bộ Refresh Token
        refreshTokenRepository.deleteByUserId(user.getId());

        userRepository.save(user);

        sendEmail(email, "VitaTrack – Mật khẩu đã thay đổi",
                "Mật khẩu tài khoản của bạn vừa được thay đổi. "
              + "Nếu bạn không thực hiện thao tác này, hãy liên hệ ngay với chúng tôi.");

        log.info("[Auth] Password reset via OTP for email={}", email);
    }

    // ═══════════════════════════════════════════════════════════════
    // FR-05  Logout / Logout All
    // ═══════════════════════════════════════════════════════════════

    /** FR-05.Logout – Xoá refresh token hiện tại */
    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * FR-05.LogoutAll – Vô hiệu hoá toàn bộ JWT/RefreshToken trên mọi thiết bị.
     * Gửi email thông báo.
     */
    @Override
    public void logoutAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);

        userRepository.findById(userId).ifPresent(user -> {
            sendEmail(user.getEmail(), "VitaTrack – Đã đăng xuất toàn bộ thiết bị",
                    "Toàn bộ phiên đăng nhập của bạn đã bị đăng xuất. "
                  + "Nếu bạn không thực hiện thao tác này, hãy đặt lại mật khẩu ngay.");
        });
    }

    // ═══════════════════════════════════════════════════════════════
    // FR-02.SessionExpiry  Refresh token
    // ═══════════════════════════════════════════════════════════════

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ."));

        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new IllegalArgumentException("Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        }

        return buildAuthResponse(rt.getUser(), false);
    }

    // ═══════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * FR-01.ValidatePassword – mật khẩu phải ≥ 8 ký tự, có chữ hoa và số.
     * Áp dụng cả đăng ký lẫn đặt lại mật khẩu.
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException(
                "Mật khẩu cần ít nhất 8 ký tự, 1 chữ hoa và 1 chữ số.");
        }
    }

    /**
     * FR-02.RateLimit – Tăng bộ đếm sai mật khẩu.
     * Sau MAX_LOGIN_ATTEMPTS lần → khoá LOCK_DURATION_MINUTES phút.
     */
    private void incrementLoginAttempts(User user) {
        int attempts = user.getLoginAttempts() + 1;
        user.setLoginAttempts(attempts);
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLoginLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("[Auth] Account temporarily locked: email={}", user.getEmail());
        }
        userRepository.save(user);
    }

    /**
     * FR-01.ResendLimit – Kiểm tra và tăng bộ đếm gửi lại xác thực.
     * Cửa sổ 1 giờ; tối đa MAX_RESEND_PER_HOUR lần.
     */
    private void checkAndIncrementResendCount(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = user.getVerificationResendWindowStart();

        if (windowStart == null || now.isAfter(windowStart.plusHours(1))) {
            // Cửa sổ mới
            user.setVerificationResendCount(1);
            user.setVerificationResendWindowStart(now);
        } else {
            if (user.getVerificationResendCount() >= MAX_RESEND_PER_HOUR) {
                throw new VitaTrackException.ResendLimitExceededException();
            }
            user.setVerificationResendCount(user.getVerificationResendCount() + 1);
        }
        userRepository.save(user);
    }

    /**
     * FR-04.OTPRules – Kiểm tra và tăng bộ đếm gửi OTP.
     * Cửa sổ 1 giờ; tối đa MAX_OTP_SEND_PER_HOUR lần.
     */
    private void checkAndIncrementOtpSendCount(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = user.getOtpWindowStart();

        if (windowStart == null || now.isAfter(windowStart.plusHours(1))) {
            user.setOtpSendCount(1);
            user.setOtpWindowStart(now);
        } else {
            if (user.getOtpSendCount() >= MAX_OTP_SEND_PER_HOUR) {
                throw new VitaTrackException.ResendLimitExceededException();
            }
            user.setOtpSendCount(user.getOtpSendCount() + 1);
        }
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, boolean rememberMe) {
        String accessToken  = jwtService.generateToken(user);
        String refreshValue = createRefreshToken(user, rememberMe);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshValue)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name().toLowerCase())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private String createRefreshToken(User user, boolean rememberMe) {
        refreshTokenRepository.deleteByUserId(user.getId());

        // RememberMe = 30 ngày; bình thường = 24 giờ (FR-02.Authenticate)
        long expirySeconds = rememberMe ? (refreshExpirationMs / 1000) : 86400;

        String tokenValue = UUID.randomUUID().toString();
        RefreshToken rt = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(expirySeconds))
                .build();
        refreshTokenRepository.save(rt);
        return tokenValue;
    }

    private void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        sendEmail(to, "VitaTrack – Xác thực tài khoản",
                "Nhấn vào liên kết sau để xác thực tài khoản (hiệu lực 24 giờ):\n\n"
              + link + "\n\nNếu bạn không đăng ký, hãy bỏ qua email này.");
    }

    private void sendOtpEmail(String to, String otp) {
        sendEmail(to, "VitaTrack – Mã xác nhận đặt lại mật khẩu",
                "Mã OTP của bạn là: " + otp
              + "\n\nMã có hiệu lực trong 10 phút và chỉ dùng được 1 lần."
              + "\nNếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.");
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("[Auth] Email sending failed to={}: {}", to, e.getMessage());
        }
    }
}
