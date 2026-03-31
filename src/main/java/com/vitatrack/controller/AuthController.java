package com.vitatrack.controller;

import com.vitatrack.dto.auth.AuthResponse;
import com.vitatrack.dto.auth.LoginRequest;
import com.vitatrack.dto.auth.RegisterRequest;
import com.vitatrack.entity.User;
import com.vitatrack.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController – endpoints đầy đủ theo SRS FR-01 → FR-05:
 *
 *  POST /auth/register               FR-01 Đăng ký bằng email
 *  GET  /auth/verify-email?token=    FR-01.EmailVerification
 *  POST /auth/resend-verification    FR-01.ResendLimit
 *  POST /auth/login                  FR-02 Đăng nhập
 *  POST /auth/google                 FR-03 Google SSO (callback từ FE)
 *  POST /auth/otp/send               FR-04.Step1 Gửi OTP 6 số
 *  POST /auth/otp/verify             FR-04.Step2 Xác nhận OTP
 *  POST /auth/otp/reset-password     FR-04.Step3 Đặt lại mật khẩu
 *  POST /auth/refresh                FR-02.SessionExpiry
 *  POST /auth/logout                 FR-05.Logout
 *  POST /auth/logout-all             FR-05.LogoutAll
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── FR-01 Register ────────────────────────────────────────────────────

    /**
     * POST /auth/register
     * Body: { email, password, fullName }
     * Response 201: "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                    "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."));
    }

    // ── FR-01.EmailVerification ───────────────────────────────────────────

    /**
     * GET /auth/verify-email?token=xxx
     * Link kích hoạt trong email – hiệu lực 24h, dùng 1 lần.
     * Thành công → redirect FE login + "Tài khoản đã được kích hoạt thành công."
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message",
            "Tài khoản đã được kích hoạt thành công. Vui lòng đăng nhập."));
    }

    // ── FR-01.ResendLimit ─────────────────────────────────────────────────

    /**
     * POST /auth/resend-verification
     * Body: { email }
     * Luôn trả 200 dù email tồn tại hay không (tránh enum).
     * Max 3 lần/giờ; vượt → 429 ResendLimitExceededException.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody Map<String, String> body) {
        authService.resendVerificationEmail(body.get("email"));
        return ResponseEntity.ok(Map.of("message",
            "Nếu tài khoản tồn tại và chưa xác thực, email xác thực đã được gửi lại."));
    }

    // ── FR-02 Login ───────────────────────────────────────────────────────

    /**
     * POST /auth/login
     * Body: { email, password, rememberMe? }
     * Response: AuthResponse (accessToken + refreshToken)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── FR-03 Google SSO ──────────────────────────────────────────────────

    /**
     * POST /auth/google
     * Body: { email, fullName?, avatarUrl? }
     * FE gọi sau khi nhận Google ID Token; BE không cần gọi lại Google.
     * Thực tế production nên verify ID Token ở BE trước khi tin.
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.loginOrRegisterWithGoogle(
                body.get("email"),
                body.get("fullName"),
                body.get("avatarUrl")));
    }

    // ── FR-04 OTP Password Reset ──────────────────────────────────────────

    /**
     * POST /auth/otp/send
     * Body: { email }
     * FR-04.Step1 – Gửi OTP 6 số. Luôn trả 200 (không rò rỉ email).
     */
    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> body) {
        authService.sendOtp(body.get("email"));
        return ResponseEntity.ok(Map.of("message",
            "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP 6 số đến địa chỉ đó."));
    }

    /**
     * POST /auth/otp/verify
     * Body: { email, otp }
     * FR-04.Step2 – Xác nhận OTP; 400 nếu sai/hết hạn.
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> body) {
        authService.verifyOtp(body.get("email"), body.get("otp"));
        return ResponseEntity.ok(Map.of("message", "Mã OTP hợp lệ. Vui lòng đặt mật khẩu mới."));
    }

    /**
     * POST /auth/otp/reset-password
     * Body: { email, otp, newPassword }
     * FR-04.Step3 – Đặt lại mật khẩu; invalidate tất cả token; gửi email thông báo.
     */
    @PostMapping("/otp/reset-password")
    public ResponseEntity<Map<String, String>> resetPasswordWithOtp(@RequestBody Map<String, String> body) {
        authService.resetPasswordWithOtp(
                body.get("email"),
                body.get("otp"),
                body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message",
            "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập lại."));
    }

    // ── FR-02.SessionExpiry Refresh ───────────────────────────────────────

    /**
     * POST /auth/refresh
     * Body: { refreshToken }
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }

    // ── FR-05 Logout ──────────────────────────────────────────────────────

    /**
     * POST /auth/logout
     * Body: { refreshToken }
     * Xóa refresh token hiện tại; JWT tự expire sau 24h.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất."));
    }

    /**
     * POST /auth/logout-all
     * FR-05.LogoutAll – Vô hiệu hoá tất cả thiết bị; gửi email thông báo.
     * Yêu cầu đã đăng nhập (JWT hợp lệ).
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@AuthenticationPrincipal User user) {
        authService.logoutAll(user.getId());
        return ResponseEntity.ok(Map.of("message",
            "Đã đăng xuất toàn bộ thiết bị. Email thông báo đã được gửi."));
    }
}
