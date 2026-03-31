package com.vitatrack.service.impl;

import com.vitatrack.entity.*;
import com.vitatrack.repository.*;
import com.vitatrack.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpertServiceImpl implements ExpertService {

    private final ExpertAssignmentRepository assignmentRepository;
    private final ExpertChatRepository chatRepository;
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final MealRepository mealRepository;
    private final ActivityRepository activityRepository;

    // ── getClients ────────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getClients(Long expertId) {
        return assignmentRepository.findClientsForExpert(expertId).stream().map(ea -> {
            User client = ea.getClient();
            long unread = chatRepository.countUnreadMessages(expertId, client.getId());

            List<ExpertChat> chats = chatRepository.findConversation(expertId, client.getId());
            String lastMessage = chats.isEmpty() ? null
                    : chats.get(chats.size() - 1).getContent();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          client.getId());
            m.put("fullName",    client.getFullName());
            m.put("email",       client.getEmail());
            m.put("avatarUrl",   client.getAvatarUrl());
            m.put("initials",    initials(client.getFullName()));
            m.put("unread",      unread);
            m.put("lastMessage", lastMessage);
            return m;
        }).collect(Collectors.toList());
    }

    // ── getChatHistory ────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getChatHistory(Long expertId, Long clientId) {
        verifyAssignment(expertId, clientId);
        chatRepository.markAllAsRead(expertId, clientId);

        return chatRepository.findConversation(expertId, clientId).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        c.getId());
            m.put("role",      c.getRole());
            m.put("content",   c.getContent());
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ── sendMessage ───────────────────────────────────────────────────────

    @Override
    public Map<String, Object> sendMessage(Long expertId, Long clientId, String content) {
        verifyAssignment(expertId, clientId);

        User expert = userRepository.findById(expertId).orElseThrow();
        User client = userRepository.findById(clientId).orElseThrow();

        ExpertChat msg = ExpertChat.builder()
                .expert(expert)
                .client(client)
                .role("expert")
                .content(content)
                .isRead(false)
                .build();
        chatRepository.save(msg);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",        msg.getId());
        result.put("role",      "expert");
        result.put("content",   content);
        result.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
        return result;
    }

    // ── getMealPlans ──────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getMealPlans(Long expertId) {
        return mealPlanRepository.findByExpertIdOrderByCreatedAtDesc(expertId).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",             p.getId());
            m.put("title",          p.getTitle());
            m.put("clientId",       p.getClient() != null ? p.getClient().getId() : null);
            m.put("clientName",     p.getClient() != null ? p.getClient().getFullName() : null);
            m.put("duration",       p.getDuration());
            m.put("targetCalories", p.getTargetCalories());
            m.put("notes",          p.getNotes());
            m.put("status",         p.getStatus());
            m.put("createdAt",      p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());
    }

    // ── createMealPlan ────────────────────────────────────────────────────

    @Override
    public Map<String, Object> createMealPlan(Long expertId, Map<String, Object> data) {
        Long clientId = Long.valueOf(data.get("clientId").toString());
        verifyAssignment(expertId, clientId);

        User expert = userRepository.findById(expertId).orElseThrow();
        User client = userRepository.findById(clientId).orElseThrow();

        MealPlan plan = MealPlan.builder()
                .expert(expert)
                .client(client)
                .title((String) data.get("title"))
                .duration(data.get("duration") != null
                        ? Integer.valueOf(data.get("duration").toString()) : null)
                .targetCalories(data.get("targetCalories") != null
                        ? Integer.valueOf(data.get("targetCalories").toString()) : null)
                .notes((String) data.get("notes"))
                .status("draft")
                .build();

        mealPlanRepository.save(plan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",     plan.getId());
        result.put("title",  plan.getTitle());
        result.put("status", plan.getStatus());
        return result;
    }

    // ── sendMealPlan ──────────────────────────────────────────────────────

    @Override
    public Map<String, Object> sendMealPlan(Long expertId, Long planId) {
        MealPlan plan = mealPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Thực đơn không tồn tại."));

        if (!plan.getExpert().getId().equals(expertId)) {
            throw new RuntimeException("Không có quyền gửi thực đơn này.");
        }
        plan.setStatus("sent");
        mealPlanRepository.save(plan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",     plan.getId());
        result.put("status", "sent");
        return result;
    }

    // ── getClientDashboard ────────────────────────────────────────────────

    @Override
    public Map<String, Object> getClientDashboard(Long expertId, Long clientId) {
        verifyAssignment(expertId, clientId);

        User client = userRepository.findById(clientId).orElseThrow();

        LocalDate today = LocalDate.now();
        double caloriesIn  = mealRepository.sumCaloriesByUserAndDate(clientId, today);
        double caloriesOut = activityRepository.sumCaloriesBurnedByUserAndDate(clientId, today);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("clientId",         clientId);
        dashboard.put("fullName",         client.getFullName());
        dashboard.put("email",            client.getEmail());
        dashboard.put("caloriesInToday",  caloriesIn);
        dashboard.put("caloriesOutToday", caloriesOut);

        healthProfileRepository.findByUserId(clientId).ifPresent(p -> {
            dashboard.put("bmi",       p.getBmi());
            dashboard.put("weight",    p.getWeight());
            dashboard.put("goalType",  p.getGoalType());
            dashboard.put("bmiAtRisk", p.getBmi() != null && (p.getBmi() < 18.5 || p.getBmi() >= 30.0));
        });

        return dashboard;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * FR-29.AccessControl / FR-30.AccessControl – chuyên gia chỉ được truy cập
     * người dùng có ExpertAssignment active. Trả HTTP 403 nếu không có quyền.
     */
    private void verifyAssignment(Long expertId, Long clientId) {
        if (!assignmentRepository.existsByExpertIdAndClientIdAndStatus(expertId, clientId, "active")) {
            throw new com.vitatrack.exception.VitaTrackException.ExpertAccessDeniedException();
        }
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
