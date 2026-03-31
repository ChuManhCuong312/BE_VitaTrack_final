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
 * Integration tests cho ActivityController
 * Test Plan ref: TC-M04-001 → TC-M04-006
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ActivityController – TC-M04 Vận động & Thiết bị")
class ActivityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;

    @BeforeEach
    void setup() throws Exception {
        String email = "activity_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(email).passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Activity User").role(Role.USER).isActive(true).build());

        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();

        accessToken = "Bearer " + new ObjectMapper().readTree(resp).get("accessToken").asText();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M04-004: Nhập thủ công - chạy bộ 30 phút
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M04-004 | Chạy bộ 30 phút 70kg → caloriesBurned ≈ 280 kcal")
    void manual_running_30min_calories() throws Exception {
        Map<String, Object> body = Map.of(
            "activityType", "running",
            "durationMinutes", 30,
            "weightKg", 70,
            "activityDate", "2026-03-20",
            "syncSource", "manual"
        );

        mockMvc.perform(post("/activities")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caloriesBurned").value(closeTo(280, 10)));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M04-005: Thời lượng = 0 → 400
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M04-005 | Thời lượng vận động = 0 → 400 validation error")
    void activity_zero_duration_rejected() throws Exception {
        Map<String, Object> body = Map.of(
            "activityType", "walking",
            "durationMinutes", 0,
            "activityDate", "2026-03-20"
        );

        mockMvc.perform(post("/activities")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M04-005b: Thời lượng âm → 400
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M04-005b | BVA: Thời lượng âm → 400")
    void activity_negative_duration_rejected() throws Exception {
        Map<String, Object> body = Map.of(
            "activityType", "running",
            "durationMinutes", -10,
            "activityDate", "2026-03-20"
        );

        mockMvc.perform(post("/activities")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // Lấy danh sách activity của user → chỉ thấy của mình
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M04 | Lấy activity list → 200 array")
    void get_activities_returns_list() throws Exception {
        mockMvc.perform(get("/activities")
                .header("Authorization", accessToken)
                .param("date", "2026-03-20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────
    // Không có token → 401
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-012 | Truy cập /activities không có token → 401")
    void activities_requires_auth() throws Exception {
        mockMvc.perform(get("/activities"))
            .andExpect(status().isUnauthorized());
    }
}
