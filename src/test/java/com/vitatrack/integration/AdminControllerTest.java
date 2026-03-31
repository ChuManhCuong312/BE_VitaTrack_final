package com.vitatrack.integration;

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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho AdminController
 * Test Plan ref: TC-M07-001 → TC-M07-005
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminController – TC-M07 Quản trị hệ thống")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Long targetUserId;

    @BeforeEach
    void setup() throws Exception {
        // Admin user
        String adminEmail = "admin_test_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(adminEmail).passwordHash(passwordEncoder.encode("Admin@12345"))
            .fullName("Admin Test").role(Role.ADMIN).isActive(true).build());

        String adminResp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + adminEmail + "\",\"password\":\"Admin@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        adminToken = "Bearer " + new ObjectMapper().readTree(adminResp).get("accessToken").asText();

        // Regular user
        String userEmail = "regular_user_" + System.currentTimeMillis() + "@vitatrack.vn";
        User regularUser = userRepository.save(User.builder()
            .email(userEmail).passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Regular User").role(Role.USER).isActive(true).build());
        targetUserId = regularUser.getId();

        String userResp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + userEmail + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        userToken = "Bearer " + new ObjectMapper().readTree(userResp).get("accessToken").asText();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M07-001: Admin CRUD thực phẩm
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M07-001 | Admin thêm thực phẩm mới → 200")
    void admin_add_food() throws Exception {
        Map<String, Object> food = Map.of(
            "name", "Test Food Admin " + System.currentTimeMillis(),
            "caloriesPer100g", 150,
            "proteinG", 10, "fatG", 5, "carbsG", 15,
            "category", "Protein"
        );

        mockMvc.perform(post("/admin/foods")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(food)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("TC-M07-001 | User thường thêm thực phẩm → 403 Forbidden")
    void user_cannot_add_food() throws Exception {
        Map<String, Object> food = Map.of(
            "name", "Hack Food", "caloriesPer100g", 100,
            "proteinG", 5, "fatG", 5, "carbsG", 10
        );

        mockMvc.perform(post("/admin/foods")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(food)))
            .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M07-004: Admin khóa / mở khóa user
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M07-004 | Admin khóa user → user không login được")
    void admin_lock_user_blocks_login() throws Exception {
        // Lấy email của user bị khóa
        User target = userRepository.findById(targetUserId).orElseThrow();
        String targetEmail = target.getEmail();

        // Admin khóa user
        mockMvc.perform(put("/admin/users/" + targetUserId + "/toggle-active")
                .header("Authorization", adminToken))
            .andExpect(status().isOk());

        // User bị khóa thử đăng nhập → phải fail
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + targetEmail + "\",\"password\":\"Test@12345\"}"))
            .andExpect(status().is(anyOf(equalTo(401), equalTo(403))));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M07-005: Admin cấp quyền Expert
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M07-005 | Admin cấp quyền Expert → user role = EXPERT")
    void admin_grant_expert_role() throws Exception {
        mockMvc.perform(put("/admin/users/" + targetUserId + "/role")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"EXPERT\"}"))
            .andExpect(status().isOk());

        // Kiểm tra trong DB
        User updated = userRepository.findById(targetUserId).orElseThrow();
        Assertions.assertEquals(Role.EXPERT, updated.getRole(),
            "Role phải là EXPERT sau khi admin cấp quyền");
    }

    // ─────────────────────────────────────────────────────────────
    // Lấy danh sách users (chỉ Admin)
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M07 | Admin lấy danh sách users → 200 array")
    void admin_get_users_list() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("TC-SEC-007 | User thường lấy danh sách users → 403")
    void user_cannot_get_admin_users_list() throws Exception {
        mockMvc.perform(get("/admin/users")
                .header("Authorization", userToken))
            .andExpect(status().isForbidden());
    }
}
