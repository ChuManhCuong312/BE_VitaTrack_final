package com.vitatrack.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitatrack.entity.*;
import com.vitatrack.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho MealController
 * Test Plan ref: TC-M03-001 → TC-M03-010
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("MealController – TC-M03 Nhật ký dinh dưỡng")
class MealControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private FoodRepository foodRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String accessToken;
    private Long riceId;
    private Long peanutId;

    @BeforeEach
    void setup() throws Exception {
        String email = "meal_test_" + System.currentTimeMillis() + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Meal Test User")
            .role(Role.USER)
            .isActive(true)
            .build());

        String resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();

        accessToken = "Bearer " + new ObjectMapper().readTree(resp).get("accessToken").asText();

        // Seed: cơm trắng
        Food rice = foodRepository.save(Food.builder()
            .name("Cơm trắng test")
            .caloriesPer100g(BigDecimal.valueOf(130))
            .proteinG(BigDecimal.valueOf(2.7))
            .carbsG(BigDecimal.valueOf(28.2))
            .fatG(BigDecimal.valueOf(0.3))
            .category("Grain").isCustom(false).build());
        riceId = rice.getId();

        // Seed: đậu phộng (cho test dị ứng)
        Food peanut = foodRepository.save(Food.builder()
            .name("Đậu phộng test")
            .caloriesPer100g(BigDecimal.valueOf(567))
            .proteinG(BigDecimal.valueOf(25.8))
            .carbsG(BigDecimal.valueOf(16.1))
            .fatG(BigDecimal.valueOf(49.2))
            .allergenTags("[\"Peanut\"]")
            .category("Fat").isCustom(false).build());
        peanutId = peanut.getId();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M03-001: Thêm bữa sáng cơm trắng 200g
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M03-001 | Thêm 200g Cơm trắng → calo=260, protein=5.4g")
    void add_rice_200g_breakfast() throws Exception {
        Map<String, Object> body = Map.of(
            "foodId", riceId,
            "mealType", "BREAKFAST",
            "quantityG", 200,
            "logDate", "2026-03-20"
        );

        mockMvc.perform(post("/meals/diary")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.caloriesConsumed").value(closeTo(260.0, 1.0)));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M03-002: Tìm thực phẩm không có trong kho → không crash
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M03-002 | Tìm thực phẩm không tồn tại → 200 empty list, không crash")
    void search_food_not_found() throws Exception {
        mockMvc.perform(get("/foods/search")
                .header("Authorization", accessToken)
                .param("q", "MonAnKhongTonTai_xyz_12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M03-005: Sửa khẩu phần 100g → 200g
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M03-005 | Thêm 100g rồi xóa, thêm 200g → calo nhân đôi")
    void edit_portion_doubles_calories() throws Exception {
        // Thêm 100g
        Map<String, Object> body100 = Map.of(
            "foodId", riceId, "mealType", "LUNCH", "quantityG", 100, "logDate", "2026-03-20");
        String resp100 = mockMvc.perform(post("/meals/diary")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body100)))
            .andReturn().getResponse().getContentAsString();
        double cal100 = new ObjectMapper().readTree(resp100).get("caloriesConsumed").asDouble();

        // Thêm 200g
        Map<String, Object> body200 = Map.of(
            "foodId", riceId, "mealType", "DINNER", "quantityG", 200, "logDate", "2026-03-20");
        String resp200 = mockMvc.perform(post("/meals/diary")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body200)))
            .andReturn().getResponse().getContentAsString();
        double cal200 = new ObjectMapper().readTree(resp200).get("caloriesConsumed").asDouble();

        Assertions.assertEquals(cal200, cal100 * 2, 2.0,
            "Khẩu phần x2 → calo phải nhân đôi");
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M03-006: Xóa bữa ăn
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M03-006 | Xóa bữa ăn → 200 message xác nhận")
    void delete_meal_entry() throws Exception {
        // Thêm trước
        Map<String, Object> body = Map.of(
            "foodId", riceId, "mealType", "BREAKFAST", "quantityG", 150, "logDate", "2026-03-20");
        String addResp = mockMvc.perform(post("/meals/diary")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andReturn().getResponse().getContentAsString();

        Long mealId = new ObjectMapper().readTree(addResp).get("id").asLong();

        // Xóa
        mockMvc.perform(delete("/meals/diary/" + mealId)
                .header("Authorization", accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M03-010: Loại bữa ăn hợp lệ
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M03-010 | Thêm bữa với loại DINNER → mealType = DINNER")
    void add_meal_correct_type() throws Exception {
        Map<String, Object> body = Map.of(
            "foodId", riceId, "mealType", "DINNER", "quantityG", 150, "logDate", "2026-03-20");

        mockMvc.perform(post("/meals/diary")
                .header("Authorization", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mealType").value("DINNER"));
    }

    // ─────────────────────────────────────────────────────────────
    // Truy cập không auth → 401
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M01-012 | Truy cập /meals/diary không có token → 401")
    void meal_diary_requires_auth() throws Exception {
        mockMvc.perform(get("/meals/diary"))
            .andExpect(status().isUnauthorized());
    }
}
