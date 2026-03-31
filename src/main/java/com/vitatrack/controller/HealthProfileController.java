package com.vitatrack.controller;

import com.vitatrack.dto.health.HealthProfileDTO;
import com.vitatrack.entity.Allergy;
import com.vitatrack.entity.User;
import com.vitatrack.service.HealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HealthProfileController – endpoints đầy đủ theo SRS FR-06 → FR-11:
 *
 *  GET  /health/metrics                       FR-06 / FR-08 / FR-09
 *  POST /health/metrics                       FR-06 cập nhật hồ sơ
 *  GET  /health/metrics/history?period=       FR-10.ViewHistory
 *  GET  /health/goals                         FR-07
 *  PUT  /health/goals                         FR-07
 *  POST /health/weight                        FR-10.ManualRecord
 *  DELETE /health/weight/{recordId}           FR-10.DeleteRecord
 *  GET  /health/allergies                     FR-11 danh sách
 *  POST /health/allergies                     FR-11.CRUD thêm
 *  PUT  /health/allergies/{id}               FR-11.CRUD sửa
 *  DELETE /health/allergies/{id}             FR-11.CRUD xóa
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    // ── FR-06 / FR-08 / FR-09 ─────────────────────────────────────────────

    /** GET /health/metrics – lấy hồ sơ + BMI + BMR + TDEE */
    @GetMapping("/metrics")
    public ResponseEntity<HealthProfileDTO> getMetrics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthProfileService.getMetrics(user.getId()));
    }

    /**
     * POST /health/metrics
     * Body: { height?, weight?, age?, gender?, activityLevel?, weightNote? }
     * FR-06: validate range; FR-06.WeightSave: lưu WeightHistory; FR-08.SafetyAlert.
     */
    @PostMapping("/metrics")
    public ResponseEntity<HealthProfileDTO> saveMetrics(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(healthProfileService.saveMetrics(user.getId(), data));
    }

    // ── FR-10 Weight History ──────────────────────────────────────────────

    /**
     * GET /health/metrics/history?period=7d
     * Khoảng: 7d | 30d | 3m | 6m | all
     */
    @GetMapping("/metrics/history")
    public ResponseEntity<HealthProfileDTO> getMetricsHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(healthProfileService.getMetricsHistory(user.getId(), period));
    }

    /**
     * POST /health/weight
     * Body: { weight, note?, date? }
     * FR-10.ManualRecord – ghi cân nặng thủ công; không được là ngày tương lai.
     */
    @PostMapping("/weight")
    public ResponseEntity<HealthProfileDTO> addWeightRecord(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {
        double weight = ((Number) body.get("weight")).doubleValue();
        String note   = (String) body.get("note");
        String dateStr = (String) body.get("date");
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthProfileService.addWeightRecord(user.getId(), weight, note, date));
    }

    /**
     * DELETE /health/weight/{recordId}
     * FR-10.DeleteRecord – hiện popup xác nhận ở FE; BE chặn xóa bản ghi đang dùng.
     */
    @DeleteMapping("/weight/{recordId}")
    public ResponseEntity<Map<String, String>> deleteWeightRecord(
            @AuthenticationPrincipal User user,
            @PathVariable Long recordId) {
        healthProfileService.deleteWeightRecord(user.getId(), recordId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bản ghi cân nặng."));
    }

    // ── FR-07 Goals ───────────────────────────────────────────────────────

    /** GET /health/goals */
    @GetMapping("/goals")
    public ResponseEntity<HealthProfileDTO> getGoals(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthProfileService.getGoals(user.getId()));
    }

    /**
     * PUT /health/goals
     * Body: { goalType, targetWeight?, dailySteps?, dailyWater? }
     * FR-07.NeedTDEE: 400 nếu chưa có TDEE.
     * FR-07.ValidateTarget: targetWeight phải hợp lệ theo goalType.
     * FR-07.Field.DailyTarget: dailyCalorieGoal tính tự động, không nhận từ body.
     */
    @PutMapping("/goals")
    public ResponseEntity<HealthProfileDTO> updateGoals(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> goals) {
        return ResponseEntity.ok(healthProfileService.updateGoals(user.getId(), goals));
    }

    // ── FR-11 Allergy CRUD ────────────────────────────────────────────────

    /**
     * GET /health/allergies
     * FR-11 – danh sách dị ứng của người dùng.
     */
    @GetMapping("/allergies")
    public ResponseEntity<List<Allergy>> getAllergies(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(healthProfileService.getAllergies(user.getId()));
    }

    /**
     * POST /health/allergies
     * Body: { allergenName, severity?, note? }
     * FR-11.DuplicateCheck – 409 nếu đã có.
     */
    @PostMapping("/allergies")
    public ResponseEntity<Allergy> addAllergy(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthProfileService.addAllergy(
                        user.getId(),
                        body.get("allergenName"),
                        body.get("severity"),
                        body.get("note")));
    }

    /**
     * PUT /health/allergies/{id}
     * Body: { severity?, note? }
     */
    @PutMapping("/allergies/{id}")
    public ResponseEntity<Allergy> updateAllergy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(healthProfileService.updateAllergy(
                user.getId(), id, body.get("severity"), body.get("note")));
    }

    /**
     * DELETE /health/allergies/{id}
     * FR-11.CRUD – thay đổi có hiệu lực ngay lập tức.
     */
    @DeleteMapping("/allergies/{id}")
    public ResponseEntity<Map<String, String>> deleteAllergy(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        healthProfileService.deleteAllergy(user.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa mục dị ứng."));
    }
}
