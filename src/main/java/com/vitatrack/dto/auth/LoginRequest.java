package com.vitatrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * FR-02.Form – Đăng nhập bằng email và mật khẩu.
 * rememberMe → FR-02.Authenticate (Refresh Token 30 ngày).
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    /** FR-02.Authenticate – Remember Me → Refresh Token 30 ngày */
    private boolean rememberMe = false;
}
