package com.vitatrack.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MealResponse – bổ sung:
 *  • largeQuantityWarning  FR-13.InvalidQuantity (khẩu phần > 5000g, FE hỏi xác nhận)
 *  • missingNutrition      FR-14.MissingNutrition ((*) thực phẩm thiếu dữ liệu calo)
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MealResponse {

    private Long      id;
    private Long      foodId;
    private String    foodName;
    private String    mealType;
    private LocalDate date;
    private double    quantity;
    private String    unit;
    private Double    calories;
    private Double    protein;
    private Double    carbs;
    private Double    fat;
    private LocalDateTime createdAt;
    private String    time;
    private boolean   addedViaAi;

    // ── FR-18 Allergy ─────────────────────────────────────────────────────
    /** true nếu thực phẩm có chứa allergen của user */
    private boolean      allergyWarning;
    /** Severity cao nhất: LOW | MEDIUM | HIGH – FE dùng để chọn toast hay modal */
    private String       allergySeverity;
    /** Danh sách tên allergen khớp */
    private List<String> matchedAllergens;

    // ── FR-13.InvalidQuantity ─────────────────────────────────────────────
    /**
     * true khi quantity > 5000g.
     * FE nhận flag này để hỏi xác nhận "Khẩu phần này có vẻ lớn bất thường. Bạn có chắc chắn?"
     * trước khi lưu (FE tự xử lý confirm dialog, BE đã lưu nhưng trả flag).
     */
    private boolean largeQuantityWarning;

    // ── FR-14.MissingNutrition ────────────────────────────────────────────
    /**
     * true khi calories_per_100g = NULL trong database.
     * FE hiển thị (*) bên cạnh tên thực phẩm.
     */
    private boolean missingNutrition;
}
