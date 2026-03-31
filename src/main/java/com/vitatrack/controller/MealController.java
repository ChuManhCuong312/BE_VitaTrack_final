package com.vitatrack.controller;

import com.vitatrack.dto.meal.AddMealDTO;
import com.vitatrack.dto.meal.FoodSearchDTO;
import com.vitatrack.dto.meal.MealResponse;
import com.vitatrack.entity.User;
import com.vitatrack.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * MealController – endpoints đầy đủ theo SRS FR-12 → FR-19:
 *
 *  GET  /meals/diary?date=           FR-19.MealList / FR-19.DateNavigator
 *  POST /meals/diary                 FR-12 / FR-13 / FR-18 tạo bữa ăn + thêm thực phẩm
 *  PUT  /meals/diary/{entryId}       FR-12.Edit / FR-13.EditItem
 *  DELETE /meals/diary/{entryId}     FR-12.Delete / FR-13.DeleteItem
 *  GET  /meals/foods/search?query=   FR-13.SearchBox
 *  GET  /meals/foods/{foodId}        FR-13.SearchResult
 *  POST /meals/foods/custom          UC-11 thực phẩm tùy chỉnh
 *  POST /meals/recognize             FR-16 / FR-17 AI Vision
 *  GET  /meals/stats?period=         FR-15 thống kê macro
 */
@RestController
@RequestMapping("/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    // ── FR-19 Diary / Dashboard ───────────────────────────────────────────

    /**
     * GET /meals/diary?date=2025-01-15
     * Trả danh sách bữa ăn trong ngày (kèm allergyWarning flag cho FR-18).
     * Nếu không truyền date → hôm nay.
     */
    @GetMapping("/diary")
    public ResponseEntity<List<MealResponse>> getDiary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(mealService.getDiary(user.getId(), date));
    }

    /**
     * POST /meals/diary
     * Body: AddMealDTO { foodId, quantity, mealType, date?, unit?, addedViaAi? }
     * FR-12.FutureDateError – 400 nếu date > today.
     * FR-13.InvalidQuantity – 400 nếu ≤ 0; flag largeQuantityWarning nếu > 5000g.
     * FR-18.SevereAllergyBlock – 422 nếu severity=HIGH.
     */
    @PostMapping("/diary")
    public ResponseEntity<MealResponse> addFoodEntry(
            @AuthenticationPrincipal User user,
            @RequestBody AddMealDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealService.addFoodEntry(user.getId(), dto));
    }

    /**
     * DELETE /meals/diary/{entryId}
     * FR-13.DeleteItem – Undo trong 5 giây (FE xử lý client-side).
     * BE xóa ngay + tính lại DailyNutritionLog.
     */
    @DeleteMapping("/diary/{entryId}")
    public ResponseEntity<Map<String, String>> deleteFoodEntry(
            @AuthenticationPrincipal User user,
            @PathVariable Long entryId) {
        mealService.deleteFoodEntry(user.getId(), entryId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bản ghi."));
    }

    // ── FR-13 Food Search ─────────────────────────────────────────────────

    /**
     * GET /meals/foods/search?query=cơm&page=0&size=10
     * FR-13.SearchBox – tối thiểu 2 ký tự, trả trong 1 giây (PER-03).
     * FR-13.NotFound – 200 với page rỗng nếu không có kết quả.
     */
    @GetMapping("/foods/search")
    public ResponseEntity<Page<FoodSearchDTO>> searchFood(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mealService.searchFood(query, page, size));
    }

    /** GET /meals/foods/{foodId} – chi tiết thực phẩm */
    @GetMapping("/foods/{foodId}")
    public ResponseEntity<FoodSearchDTO> getFoodDetail(@PathVariable Long foodId) {
        return ResponseEntity.ok(mealService.getFoodDetail(foodId));
    }

    /**
     * POST /meals/foods/custom
     * Body: { name, caloriesPer100g, proteinPer100g?, carbsPer100g?, fatPer100g?,
     *          category?, allergens? }
     * FR-13 / UC-11 – thêm thực phẩm tùy chỉnh khi không tìm thấy trong kho.
     */
    @PostMapping("/foods/custom")
    public ResponseEntity<FoodSearchDTO> addCustomFood(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealService.addCustomFood(user.getId(), data));
    }

    // ── FR-16 / FR-17 AI Vision ───────────────────────────────────────────

    /**
     * POST /meals/recognize  (multipart/form-data, field "image")
     * FR-16.Validation – kiểm tra định dạng + kích thước.
     * FR-16.Timeout    – 10 giây → 504 VisionTimeoutException.
     * FR-16.APIError   – lỗi API → 503 VisionApiException.
     * FR-17.LowConfidence – confidence < 60% → flag lowConfidenceWarning=true.
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognizeFood(
            @RequestParam("image") MultipartFile image) {
        return ResponseEntity.ok(mealService.recognizeFood(image));
    }

    // ── FR-15 Stats ───────────────────────────────────────────────────────

    /**
     * GET /meals/stats?period=7d
     * Khoảng: 7d | 30d | 3m – tổng calo + macro từng ngày.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "7d") String period) {
        return ResponseEntity.ok(mealService.getStats(user.getId(), period));
    }
}
