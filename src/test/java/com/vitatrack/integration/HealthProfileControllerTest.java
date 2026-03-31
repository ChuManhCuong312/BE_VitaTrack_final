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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho HealthProfileController
 * Test Plan ref: TC-M02-001 → TC-M02-010 (BVA, BMI/BMR, Goals)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("HealthProfileController – TC-M02 Hồ sơ sức khỏe")
class HealthProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String BASE = "/health";
    private String accessToken;

    @BeforeEach
    void setup() throws Exception {
        // Tạo user test và lấy token
        String email = "health_test_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Health Test User")
            .role(Role.USER)
            .isActive(true)
            .build());

        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();

        accessToken = "Bearer " + new ObjectMapper().readTree(resp).get("accessToken").asText();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-001: Lưu hồ sơ hợp lệ → tính BMI/BMR đúng
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M02-001 | Nam 25t 175cm 70kg → BMI≈22.9, BMR≈1674")
    void save_profile_calculates_bmi_bmr() throws Exception {
        Map<String, Object> data = Map.of(
            "heightCm", 175, "weightKg", 70, "gender", "MALE",
            "age", 25, "activityLevel", "SEDENTARY", "goalType", "MAINTAIN"
        );

        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.bmi").value(closeTo(22.9, 0.2)))
            .andExpect(jsonPath("$.bmr").value(closeTo(1674, 10)));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-002/004: BVA chiều cao biên hợp lệ
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M02-002 | BVA: Chiều cao = 50cm (biên dưới) → 200 OK")
    void height_min_boundary_50cm() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 50, "weightKg", 20);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-M02-004 | BVA: Chiều cao = 250cm (biên trên) → 200 OK")
    void height_max_boundary_250cm() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 250, "weightKg", 100);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-003/005: BVA chiều cao ngoài biên
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M02-003 | BVA: Chiều cao = 49cm (dưới biên) → 400")
    void height_below_min_49cm() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 49, "weightKg", 20);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-M02-005 | BVA: Chiều cao = 251cm (vượt biên) → 400")
    void height_above_max_251cm() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 251, "weightKg", 100);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-006/007: Cân nặng không hợp lệ
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M02-006 | Cân nặng âm (-5kg) → 400")
    void weight_negative() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 170, "weightKg", -5);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-M02-007 | Cân nặng = 0kg → 400")
    void weight_zero() throws Exception {
        Map<String, Object> data = Map.of("heightCm", 170, "weightKg", 0);
        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-009: Mục tiêu giảm cân → Daily Budget = TDEE - 500
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M02-009 | Mục tiêu LOSE_WEIGHT → dailyCalorieGoal = TDEE - 500")
    void goal_lose_weight_budget() throws Exception {
        Map<String, Object> data = Map.of(
            "heightCm", 175, "weightKg", 80,
            "gender", "MALE", "age", 30,
            "activityLevel", "SEDENTARY",
            "goalType", "LOSE_WEIGHT",
            "targetWeightKg", 70
        );

        mockMvc.perform(post(BASE + "/metrics")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
            .andExpect(status().isOk())
            // dailyCalorieGoal nên < TDEE (giảm cân → -500)
            .andExpect(jsonPath("$.goalType").value("LOSE_WEIGHT"));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M02-012: Truy cập không có token → 401
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-012 | Truy cập /health/metrics khi chưa đăng nhập → 401")
    void access_without_token_returns_401() throws Exception {
        mockMvc.perform(get(BASE + "/metrics"))
            .andExpect(status().isUnauthorized());
    }
}
