package com.vitatrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests – OWASP Top 10 mapping
 * Test Plan ref: TC-SEC-001 → TC-SEC-010, mục 5.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Tests – OWASP Top 10")
class SecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String userAToken;
    private Long userBId;

    @BeforeEach
    void setup() throws Exception {
        // User A
        String emailA = "sec_userA_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(emailA).passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("User A").role(Role.USER).isActive(true).build());

        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + emailA + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        userAToken = "Bearer " + new ObjectMapper().readTree(resp).get("accessToken").asText();

        // User B (victim)
        User userB = userRepository.save(User.builder()
            .email("sec_userB_" + System.currentTimeMillis() + "@vitatrack.vn")
            .passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("User B").role(Role.USER).isActive(true).build());
        userBId = userB.getId();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-001: Truy cập không có token → 401
    // OWASP A01 – Broken Access Control
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-001 | A01 BAC | GET /users/{id} không có token → 401")
    void access_without_token_401() throws Exception {
        mockMvc.perform(get("/users/" + userBId))
            .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-002: User A truy cập data User B (IDOR)
    // OWASP A01 – IDOR
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-002 | A01 IDOR | User A truy cập profile User B → 403")
    void idor_user_cannot_access_other_user_profile() throws Exception {
        mockMvc.perform(get("/users/" + userBId)
                .header("Authorization", userAToken))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-003: SQL Injection tại ô tìm kiếm
    // OWASP A03 – Injection
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-003 | A03 SQLi | SQL injection trong search → không crash, không execute")
    void sql_injection_search_is_safe() throws Exception {
        String sqlPayload = "'; DROP TABLE users; --";
        mockMvc.perform(get("/foods/search")
                .header("Authorization", userAToken)
                .param("q", sqlPayload))
            // Không crash (không 500 do SQL error)
            .andExpect(status().is(not(500)))
            // Kết quả phải là danh sách rỗng hoặc OK, không được lộ SQL error
            .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-004: XSS trong input fields
    // OWASP A03 – XSS
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-004 | A03 XSS | Script tag trong fullName → không thực thi, được encode")
    void xss_script_in_name_is_encoded() throws Exception {
        String email = "xss_test_" + System.currentTimeMillis() + "@vitatrack.vn";
        String body = String.format(
            "{\"email\":\"%s\",\"password\":\"Test@12345\",\"fullName\":\"<script>alert('xss')</script>\"}",
            email);

        String resp = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn().getResponse().getContentAsString();

        // fullName trong response không được chứa thẻ script chưa encode
        Assertions.assertFalse(resp.contains("<script>"),
            "Response không được chứa thẻ <script> chưa encode");
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-005: Mật khẩu phải được hash trong DB
    // OWASP A02 – Cryptographic Failures
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-005 | A02 Crypto | Mật khẩu trong DB phải là bcrypt hash, không phải plaintext")
    void password_stored_as_hash() {
        User user = userRepository.findByEmail(
            userRepository.findAll().stream()
                .filter(u -> u.getEmail().startsWith("sec_userA_"))
                .findFirst().map(User::getEmail).orElse(""))
            .orElse(null);

        Assertions.assertNotNull(user, "User A phải tồn tại");
        String hash = user.getPasswordHash();

        // BCrypt hash bắt đầu bằng $2a$ hoặc $2b$
        Assertions.assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
            "Password phải là BCrypt hash, bắt đầu bằng $2a$ hoặc $2b$");
        Assertions.assertNotEquals("Test@12345", hash,
            "Password không được lưu dạng plaintext");
        Assertions.assertTrue(hash.length() >= 60,
            "BCrypt hash phải có độ dài ít nhất 60 ký tự");
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-007: User gọi Admin API → 403
    // OWASP A01 – Authorization
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-007 | A01 AuthZ | User thường gọi Admin API → 403")
    void user_cannot_call_admin_api() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", userAToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-SEC-007b | User gọi POST /admin/foods → 403")
    void user_cannot_create_food_as_admin() throws Exception {
        String body = "{\"name\":\"Hack Food\",\"caloriesPer100g\":100,\"proteinG\":10,\"fatG\":5,\"carbsG\":10}";
        mockMvc.perform(post("/admin/foods")
                .header("Authorization", userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-008: Response lỗi không lộ stack trace
    // OWASP A05 – Security Misconfiguration
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-008 | A05 | Response lỗi không lộ stack trace / DB info")
    void error_response_does_not_expose_stack_trace() throws Exception {
        // Gửi request thiếu body để gây lỗi
        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andReturn().getResponse().getContentAsString();

        Assertions.assertFalse(resp.contains("at com.vitatrack"),
            "Stack trace không được lộ trong response");
        Assertions.assertFalse(resp.contains("Hibernate"),
            "Thông tin ORM không được lộ");
        Assertions.assertFalse(resp.contains("SQL"),
            "SQL không được lộ trong error response");
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-009: Đăng nhập sai ≥5 lần → lockout
    // OWASP A07 – Identification & Auth Failures
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-009 | A07 | Đăng nhập sai 5 lần → tài khoản bị khóa (lockout)")
    void login_lockout_after_5_failed_attempts() throws Exception {
        String email = "lockout_test_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(email).passwordHash(passwordEncoder.encode("Correct@123"))
            .fullName("Lockout User").role(Role.USER).isActive(true).build());

        String wrongBody = String.format("{\"email\":\"%s\",\"password\":\"WRONG_PASSWORD\"}", email);

        // 5 lần đăng nhập sai
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(wrongBody));
        }

        // Lần 6 (kể cả đúng password) phải trả 401/403 do bị khóa
        String correctBody = String.format("{\"email\":\"%s\",\"password\":\"Correct@123\"}", email);
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctBody))
            .andExpect(status().is(anyOf(equalTo(401), equalTo(403), equalTo(423))));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-SEC-010: Refresh token bị vô hiệu hóa sau logout
    // OWASP A07 – Token invalidation
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-SEC-010 | A07 | Refresh token không thể dùng sau khi logout")
    void refresh_token_invalidated_after_logout() throws Exception {
        String email = "token_inv_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(email).passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Token Test").role(Role.USER).isActive(true).build());

        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();

        String rt = new ObjectMapper().readTree(resp).get("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + rt + "\"}"));

        // Thử refresh sau logout → phải fail
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rt + "\"}"))
            .andExpect(status().isBadRequest());
    }
}
