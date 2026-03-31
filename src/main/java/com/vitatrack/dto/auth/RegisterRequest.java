package com.vitatrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * FR-01.Form – Đăng ký tài khoản bằng email.
 * Validation tối thiểu ở tầng DTO; rule nghiệp vụ chi tiết (chữ hoa, số)
 * được kiểm tra trong AuthServiceImpl.validatePasswordStrength().
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    /** FR-01.ValidatePassword – min 8, có chữ hoa và số; kiểm tra chi tiết trong service */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu cần ít nhất 8 ký tự, 1 chữ hoa và 1 chữ số.")
    private String password;

    /** FR-01.Form – Họ và tên: 2-100 ký tự */
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự.")
    private String fullName;
}
