package com.vitatrack.controller;

import com.vitatrack.entity.User;
import com.vitatrack.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertService expertService;

    /** GET /expert/clients */
    @GetMapping("/clients")
    public ResponseEntity<List<Map<String, Object>>> getClients(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expertService.getClients(user.getId()));
    }

    /** GET /expert/chat/:clientId */
    @GetMapping("/chat/{clientId}")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long clientId) {
        return ResponseEntity.ok(expertService.getChatHistory(user.getId(), clientId));
    }

    /** POST /expert/chat/:clientId  { content } */
    @PostMapping("/chat/{clientId}")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long clientId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(expertService.sendMessage(user.getId(), clientId, body.get("content")));
    }

    /** GET /expert/meal-plans */
    @GetMapping("/meal-plans")
    public ResponseEntity<List<Map<String, Object>>> getMealPlans(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(expertService.getMealPlans(user.getId()));
    }

    /** POST /expert/meal-plans */
    @PostMapping("/meal-plans")
    public ResponseEntity<Map<String, Object>> createMealPlan(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(expertService.createMealPlan(user.getId(), data));
    }

    /** POST /expert/meal-plans/:id/send */
    @PostMapping("/meal-plans/{id}/send")
    public ResponseEntity<Map<String, Object>> sendMealPlan(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(expertService.sendMealPlan(user.getId(), id));
    }

    /** GET /expert/clients/:clientId/dashboard */
    @GetMapping("/clients/{clientId}/dashboard")
    public ResponseEntity<Map<String, Object>> getClientDashboard(
            @AuthenticationPrincipal User user,
            @PathVariable Long clientId) {
        return ResponseEntity.ok(expertService.getClientDashboard(user.getId(), clientId));
    }
}

// ── additional ExpertController endpoints appended ──
// Note: the above file already handles all core endpoints.
// FR-29.AccessControl is enforced in ExpertServiceImpl.verifyAssignment()
// which now throws ExpertAccessDeniedException (HTTP 403) – handled by GlobalExceptionHandler.
