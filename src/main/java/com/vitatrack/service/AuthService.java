package com.vitatrack.service;

import com.vitatrack.dto.auth.AuthResponse;
import com.vitatrack.dto.auth.LoginRequest;
import com.vitatrack.dto.auth.RegisterRequest;

/**
 * AuthService – interface đầy đủ theo SRS FR-01 → FR-05.
 */
public interface AuthService {

    // FR-01 Register
    void register(RegisterRequest request);

    // FR-01.EmailVerification
    void verifyEmail(String token);

    // FR-01.ResendLimit
    void resendVerificationEmail(String email);

    // FR-02 Login
    AuthResponse login(LoginRequest request);

    // FR-03 Google SSO
    AuthResponse loginOrRegisterWithGoogle(String email, String fullName, String avatarUrl);

    // FR-04 OTP Password Reset
    void sendOtp(String email);
    void verifyOtp(String email, String otp);
    void resetPasswordWithOtp(String email, String otp, String newPassword);

    // FR-02.SessionExpiry
    AuthResponse refreshToken(String refreshToken);

    // FR-05 Logout
    void logout(String refreshToken);

    // FR-05.LogoutAll
    void logoutAll(Long userId);
}
