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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests cho ExpertController
 * Test Plan ref: TC-M06-001 → TC-M06-005, TC-SEC-006 (AuthZ)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ExpertController – TC-M06 Tương tác chuyên gia")
class ExpertControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String expert1Token;
    private String expert2Token;
    private String userToken;
    private Long userId;

    @BeforeEach
    void setup() throws Exception {
        long ts = System.currentTimeMillis();

        // Expert 1
        String e1Email = "expert1_" + ts + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(e1Email).passwordHash(passwordEncoder.encode("Expert@12345"))
            .fullName("Expert 1").role(Role.EXPERT).isActive(true)
            .expertStatus("verified").build());

        String e1Resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e1Email + "\",\"password\":\"Expert@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        expert1Token = "Bearer " + new ObjectMapper().readTree(e1Resp).get("accessToken").asText();

        // Expert 2
        String e2Email = "expert2_" + ts + "@vitatrack.vn";
        userRepository.save(User.builder()
            .email(e2Email).passwordHash(passwordEncoder.encode("Expert@12345"))
            .fullName("Expert 2").role(Role.EXPERT).isActive(true)
            .expertStatus("verified").build());

        String e2Resp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + e2Email + "\",\"password\":\"Expert@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        expert2Token = "Bearer " + new ObjectMapper().readTree(e2Resp).get("accessToken").asText();

        // Regular user
        String uEmail = "user_exp_" + ts + "@vitatrack.vn";
        User user = userRepository.save(User.builder()
            .email(uEmail).passwordHash(passwordEncoder.encode("Test@12345"))
            .fullName("Regular User").role(Role.USER).isActive(true).build());
        userId = user.getId();

        String uResp = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + uEmail + "\",\"password\":\"Test@12345\"}"))
            .andReturn().getResponse().getContentAsString();
        userToken = "Bearer " + new ObjectMapper().readTree(uResp).get("accessToken").asText();
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M06-001: User xem danh sách chuyên gia được gán
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M06-001 | User xem danh sách expert được gán → 200 array")
    void user_views_assigned_experts() throws Exception {
        mockMvc.perform(get("/expert/my-experts")
                .header("Authorization", userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M06-003: Expert xem danh sách user được gán
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M06-003 | Expert xem danh sách user được assign → 200 array")
    void expert_views_assigned_users() throws Exception {
        mockMvc.perform(get("/expert/my-clients")
                .header("Authorization", expert1Token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M06-004: Expert A KHÔNG xem được data của Expert B
    // OWASP A01 – Broken Access Control
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M06-004 | A01 | Expert A truy cập user của Expert B → 403")
    void expert_cannot_access_other_expert_clients() throws Exception {
        // Expert 2 thử xem danh sách clients của Expert 1
        // Lấy id của expert 1 từ DB
        User expert1 = userRepository.findAll().stream()
            .filter(u -> u.getEmail().contains("expert1_"))
            .findFirst().orElse(null);

        if (expert1 != null) {
            mockMvc.perform(get("/expert/clients/" + expert1.getId())
                    .header("Authorization", expert2Token))
                .andExpect(status().is(anyOf(equalTo(403), equalTo(404))));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TC-M06-002: Chat user ↔ chuyên gia
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M06-002 | User gửi tin nhắn cho expert → 200")
    void user_send_message_to_expert() throws Exception {
        User expert1 = userRepository.findAll().stream()
            .filter(u -> u.getEmail().contains("expert1_"))
            .findFirst().orElseThrow();

        String body = String.format(
            "{\"receiverId\":%d, \"message\":\"Xin chào bác sĩ, tôi cần tư vấn dinh dưỡng.\"}",
            expert1.getId()
        );

        mockMvc.perform(post("/expert/chat")
                .header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────
    // User role không được truy cập expert-only endpoint
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-M06 | User thường truy cập expert endpoint → 403")
    void user_cannot_access_expert_endpoint() throws Exception {
        mockMvc.perform(get("/expert/my-clients")
                .header("Authorization", userToken))
            .andExpect(status().isForbidden());
    }
}
