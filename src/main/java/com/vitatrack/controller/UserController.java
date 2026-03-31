package com.vitatrack.controller;

import com.vitatrack.entity.ExpertChat;
import com.vitatrack.entity.User;
import com.vitatrack.repository.ExpertAssignmentRepository;
import com.vitatrack.repository.ExpertChatRepository;
import com.vitatrack.repository.MealPlanRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UserController – quản lý hồ sơ cá nhân người dùng.
 *
 *  GET  /users/me                   – lấy profile hiện tại
 *  PUT  /users/me                   – cập nhật tên
 *  POST /users/me/password          – đổi mật khẩu
 *  POST /users/me/avatar            – upload avatar
 *  GET  /users/me/expert            – chuyên gia đang phụ trách mình
 *  GET  /users/me/chat/{expertId}   – lịch sử chat với chuyên gia
 *  POST /users/me/chat/{expertId}   – gửi tin nhắn đến chuyên gia
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService              userService;
    private final ExpertAssignmentRepository assignmentRepository;
    private final ExpertChatRepository     chatRepository;
    private final MealPlanRepository       mealPlanRepository;
    private final UserRepository           userRepository;

    // ── Profile ───────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMe(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), body));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        userService.changePassword(user.getId(), body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(Map.of("message", "Đã đổi mật khẩu thành công."));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("avatar") MultipartFile file) {
        String url = userService.uploadAvatar(user.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", url));
    }

    // ── User – Expert Interaction ─────────────────────────────────────────

    /**
     * GET /users/me/expert
     * Trả về thông tin chuyên gia đang được phân công cho người dùng này.
     * Nếu chưa có phân công → { assigned: false }
     */
    @GetMapping("/me/expert")
    public ResponseEntity<Map<String, Object>> getMyExpert(@AuthenticationPrincipal User user) {
        var assignments = assignmentRepository.findByClientIdAndStatus(user.getId(), "active");
        if (assignments.isEmpty()) {
            return ResponseEntity.ok(Map.of("assigned", false));
        }
        var expert = assignments.get(0).getExpert();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assigned",   true);
        m.put("id",         expert.getId());
        m.put("fullName",   expert.getFullName());
        m.put("email",      expert.getEmail());
        m.put("avatarUrl",  expert.getAvatarUrl());
        m.put("assignedAt", assignments.get(0).getAssignedAt().toString());
        return ResponseEntity.ok(m);
    }

    /**
     * GET /users/me/chat/{expertId}
     * Người dùng xem lịch sử chat với chuyên gia được phân công.
     * 403 nếu chuyên gia này chưa được phân công cho user.
     */
    @GetMapping("/me/chat/{expertId}")
    public ResponseEntity<List<Map<String, Object>>> getChatWithExpert(
            @AuthenticationPrincipal User user,
            @PathVariable Long expertId) {
        if (!assignmentRepository.existsByExpertIdAndClientIdAndStatus(expertId, user.getId(), "active")) {
            return ResponseEntity.status(403).body(
                List.of(Map.of("error", "Chuyên gia này chưa được phân công cho bạn.")));
        }
        // Đánh dấu đã đọc khi user mở chat
        chatRepository.markAllAsRead(expertId, user.getId());

        List<Map<String, Object>> result = chatRepository
                .findConversation(expertId, user.getId())
                .stream().map(c -> {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("id",        c.getId());
                    mm.put("role",      c.getRole());
                    mm.put("content",   c.getContent());
                    mm.put("isRead",    c.isRead());
                    mm.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
                    return mm;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /users/me/chat/{expertId}
     * Người dùng gửi tin nhắn đến chuyên gia.
     * Lưu với role="client" để phân biệt với tin nhắn của chuyên gia (role="expert").
     */
    /**
     * GET /users/me/meal-plans
     * Người dùng xem danh sách thực đơn được chuyên gia gửi (status='sent').
     */
    @GetMapping("/me/meal-plans")
    public ResponseEntity<List<Map<String, Object>>> getMyMealPlans(@AuthenticationPrincipal User user) {
        List<Map<String, Object>> plans = mealPlanRepository
                .findByClientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(p -> "sent".equals(p.getStatus()))
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",             p.getId());
                    m.put("title",          p.getTitle());
                    m.put("targetCalories", p.getTargetCalories());
                    m.put("duration",       p.getDuration());
                    m.put("notes",          p.getNotes());
                    m.put("status",         p.getStatus());
                    m.put("createdAt",      p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
                    if (p.getExpert() != null) {
                        m.put("expertName", p.getExpert().getFullName());
                        m.put("expertId",   p.getExpert().getId());
                    }
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/me/chat/{expertId}")
    public ResponseEntity<Map<String, Object>> sendToExpert(
            @AuthenticationPrincipal User user,
            @PathVariable Long expertId,
            @RequestBody Map<String, String> body) {
        if (!assignmentRepository.existsByExpertIdAndClientIdAndStatus(expertId, user.getId(), "active")) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Chuyên gia này chưa được phân công cho bạn."));
        }
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nội dung không được để trống."));
        }

        User expert = userRepository.findById(expertId).orElseThrow();
        ExpertChat msg = ExpertChat.builder()
                .expert(expert)
                .client(user)
                .role("client")
                .content(content.trim())
                .isRead(false)
                .build();
        chatRepository.save(msg);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id",        msg.getId());
        res.put("role",      "client");
        res.put("content",   content.trim());
        res.put("isRead",    false);
        res.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
        return ResponseEntity.ok(res);
    }
}
