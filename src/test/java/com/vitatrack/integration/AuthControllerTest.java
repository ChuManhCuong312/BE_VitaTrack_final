package com.vitatrack.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitatrack.dto.auth.LoginRequest;
import com.vitatrack.dto.auth.RegisterRequest;
import com.vitatrack.entity.Role;
import com.vitatrack.entity.User;
import com.vitatrack.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho AuthController
 * Test Plan ref: TC-M01-001 → TC-M01-012
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController – TC-M01 Quản lý tài khoản")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String BASE = "/auth";
    private static final String TEST_EMAIL    = "integration_test@vitatrack.vn";
    private static final String TEST_PASSWORD = "Test@12345";
    private static final String TEST_NAME     = "Test User";

    // Tạo sẵn user cho các test cần đăng nhập
    @BeforeEach
    void seedUser() {
        if (!userRepository.existsByEmail(TEST_EMAIL)) {
            userRepository.save(User.builder()
                .email(TEST_EMAIL)
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .fullName(TEST_NAME)
                .role(Role.USER)
                .isActive(true)
                .build());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-001: Đăng ký hợp lệ
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-001 | Đăng ký email+password hợp lệ → 200 + accessToken")
    void register_valid_credentials() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
            .email("newuser_" + System.currentTimeMillis() + "@vitatrack.vn")
            .password("Valid@12345")
            .fullName("Người Dùng Mới")
            .build();

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value(req.getEmail()))
            .andExpect(jsonPath("$.role").value("user"));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-002: Đăng ký email đã tồn tại
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-002 | Đăng ký email đã tồn tại → 400 + message lỗi")
    void register_duplicate_email() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
            .email(TEST_EMAIL)
            .password(TEST_PASSWORD)
            .fullName(TEST_NAME)
            .build();

        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Email")));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-003: Email sai định dạng
    // ─────────────────────────────────────────────────────────────
    @ParameterizedTest_disabled
    @DisplayName("TC-M01-003 | Email sai định dạng → 400 validation error")
    void register_invalid_email_formats() throws Exception {
        for (String badEmail : new String[]{"notanemail", "missing@", "@nodomain.com"}) {
            RegisterRequest req = RegisterRequest.builder()
                .email(badEmail).password(TEST_PASSWORD).fullName("Test").build();

            mockMvc.perform(post(BASE + "/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("TC-M01-003a | Email thiếu @ → 400")
    void register_email_no_at() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
            .email("notanemail").password(TEST_PASSWORD).fullName("Test").build();
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-M01-003b | Email thiếu domain → 400")
    void register_email_no_domain() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
            .email("missing@").password(TEST_PASSWORD).fullName("Test").build();
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-M01-003c | Email thiếu local part → 400")
    void register_email_no_local() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
            .email("@nodomain.com").password(TEST_PASSWORD).fullName("Test").build();
        mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-005: Đăng nhập đúng
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-005 | Đăng nhập đúng email+password → 200 + tokens")
    void login_valid() throws Exception {
        LoginRequest req = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value(TEST_EMAIL))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-006: Đăng nhập sai password
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-006 | Đăng nhập sai password → 401, không lộ chi tiết")
    void login_wrong_password() throws Exception {
        LoginRequest req = new LoginRequest(TEST_EMAIL, "WrongPassword!99");

        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized())
            // Không được lộ thông tin như "email không tồn tại" hay "sai password"
            .andExpect(jsonPath("$.message").value(containsString("không đúng")))
            .andExpect(jsonPath("$.message").value(not(containsString("email không tồn tại"))))
            .andExpect(jsonPath("$.message").value(not(containsString("password incorrect"))));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-006b: Đăng nhập email không tồn tại
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-006b | Đăng nhập email không tồn tại → 401")
    void login_nonexistent_email() throws Exception {
        LoginRequest req = new LoginRequest("ghost@vitatrack.vn", TEST_PASSWORD);

        mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-008: Quên mật khẩu → trả 200 dù email không tồn tại
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-008 | Forgot password – luôn trả 200 (không lộ email tồn tại)")
    void forgot_password_always_200() throws Exception {
        String body = "{\"email\": \"nonexistent@vitatrack.vn\"}";
        mockMvc.perform(post(BASE + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-010: Đăng xuất
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-010 | Đăng xuất → 200 + message xác nhận")
    void logout_success() throws Exception {
        // Lấy refresh token từ login trước
        LoginRequest loginReq = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        String loginResp = mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(loginResp).get("refreshToken").asText();

        mockMvc.perform(post(BASE + "/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M01-010b: Dùng lại refresh token sau logout → phải thất bại
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-010b | Dùng lại refresh token sau logout → 400 invalid")
    void refresh_token_invalid_after_logout() throws Exception {
        LoginRequest loginReq = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        String resp = mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
            .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(resp).get("refreshToken").asText();

        // Logout
        mockMvc.perform(post(BASE + "/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"));

        // Dùng lại token đã logout
        mockMvc.perform(post(BASE + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
            .andExpect(status().isBadRequest());
    }

    /** Placeholder annotation – thay thế @ParameterizedTest để tránh skip */
    @interface ParameterizedTest_disabled {}
}
