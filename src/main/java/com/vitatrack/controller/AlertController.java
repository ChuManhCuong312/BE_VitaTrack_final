package com.vitatrack.controller;

import com.vitatrack.entity.HealthAlert;
import com.vitatrack.entity.User;
import com.vitatrack.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AlertController – đầy đủ theo SRS FR-32:
 *
 *  GET   /alerts           Danh sách tất cả alert của user
 *  GET   /alerts/unread    Chỉ chưa đọc
 *  GET   /alerts/count     Số lượng chưa đọc (badge đỏ trên menu)
 *  PATCH /alerts/{id}/read Đánh dấu 1 alert đã đọc
 *  PATCH /alerts/read-all  Đánh dấu tất cả đã đọc
 *  POST  /alerts/{id}/acknowledge  FR-32.Acknowledge – Expert ghi nhận + nhập note
 *  POST  /alerts/evaluate  Kích hoạt AlertEngine thủ công (debug / on-demand)
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<HealthAlert>> getAll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(alertService.getAlerts(user.getId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<HealthAlert>> getUnread(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(alertService.getUnread(user.getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("unread", alertService.countUnread(user.getId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<HealthAlert> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(alertService.markRead(user.getId(), id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead(@AuthenticationPrincipal User user) {
        alertService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả đã đọc."));
    }

    /**
     * POST /alerts/{id}/acknowledge
     * FR-32.Acknowledge – Expert đánh dấu alert "Đã xử lý" + nhập ghi chú bắt buộc.
     * Body: { note: "Đã liên hệ qua chat..." }  (10-200 ký tự)
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledge(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String note = body.get("note");
        if (note == null || note.trim().length() < 10 || note.trim().length() > 200) {
            throw new IllegalArgumentException(
                "Ghi chú xử lý phải từ 10 đến 200 ký tự.");
        }
        alertService.acknowledge(user.getId(), id, note.trim());
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu cảnh báo là đã xử lý."));
    }

    /** POST /alerts/evaluate – kích hoạt AlertEngine thủ công */
    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, String>> evaluate(@AuthenticationPrincipal User user) {
        alertService.evaluate(user.getId());
        return ResponseEntity.ok(Map.of("message", "Đã đánh giá cảnh báo sức khoẻ."));
    }
}
