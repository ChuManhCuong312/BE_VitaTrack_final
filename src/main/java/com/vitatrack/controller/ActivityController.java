package com.vitatrack.controller;

import com.vitatrack.dto.activity.ActivityDTO;
import com.vitatrack.dto.activity.ActivityResponse;
import com.vitatrack.entity.User;
import com.vitatrack.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** GET /activities/today */
    @GetMapping("/today")
    public ResponseEntity<List<ActivityResponse>> getToday(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activityService.getToday(user.getId()));
    }

    /** POST /activities */
    @PostMapping
    public ResponseEntity<ActivityResponse> addActivity(@AuthenticationPrincipal User user,
                                                         @Valid @RequestBody ActivityDTO dto) {
        return ResponseEntity.ok(activityService.addActivity(user.getId(), dto));
    }

    /** DELETE /activities/:id */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteActivity(@AuthenticationPrincipal User user,
                                                               @PathVariable Long id) {
        activityService.deleteActivity(user.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa hoạt động."));
    }

    /** GET /activities/history?from=2024-01-01&to=2024-01-31 */
    @GetMapping("/history")
    public ResponseEntity<List<ActivityResponse>> getHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(activityService.getHistory(user.getId(), from, to));
    }

    /** POST /activities/sync  { device, accessToken } */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncWearable(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                activityService.syncWearable(user.getId(), body.get("device"), body.get("accessToken")));
    }

    /** GET /activities/wearable */
    @GetMapping("/wearable")
    public ResponseEntity<Map<String, Object>> getWearableStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(activityService.getWearableStatus(user.getId()));
    }

    /** GET /activities/stats?period=7d */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal User user,
                                                         @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(activityService.getStats(user.getId(), period));
    }
}
