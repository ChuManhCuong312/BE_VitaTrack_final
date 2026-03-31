package com.vitatrack.controller;

import com.vitatrack.dto.ai.AIChatRequest;
import com.vitatrack.entity.User;
import com.vitatrack.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AIController – endpoints theo SRS FR-24 → FR-27.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    // FR-24 – chat với session-based history
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(aiService.sendMessage(
                user.getId(), body.get("message"), body.get("sessionId")));
    }

    // FR-24.MaxHistory / FR-27 – lấy history theo session
    @GetMapping("/chat/history")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(aiService.getChatHistory(user.getId(), sessionId));
    }

    // FR-27.Search
    @GetMapping("/chat/search")
    public ResponseEntity<List<Map<String, Object>>> searchHistory(
            @AuthenticationPrincipal User user,
            @RequestParam String keyword) {
        return ResponseEntity.ok(aiService.searchChatHistory(user.getId(), keyword));
    }

    // FR-27.SessionGroup – danh sách session
    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.getConversations(user.getId()));
    }

    // FR-27.Delete – xóa một session
    @DeleteMapping("/conversations/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteSession(
            @AuthenticationPrincipal User user,
            @PathVariable String sessionId) {
        aiService.deleteChatSession(user.getId(), sessionId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa phiên chat."));
    }

    // FR-27.Delete – xóa toàn bộ lịch sử
    @DeleteMapping("/conversations")
    public ResponseEntity<Map<String, String>> deleteAll(@AuthenticationPrincipal User user) {
        aiService.deleteAllChatHistory(user.getId());
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ lịch sử hội thoại."));
    }

    // FR-26 Meal Plan suggestion
    @PostMapping("/meal-plan")
    public ResponseEntity<Map<String, Object>> generateMealPlan(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> preferences) {
        return ResponseEntity.ok(aiService.generateMealPlan(user.getId(), preferences));
    }

    // Dashboard suggestions
    @GetMapping("/suggestions")
    public ResponseEntity<List<Map<String, Object>>> getDailySuggestions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.getDailySuggestions(user.getId()));
    }
}
