package com.vitatrack.unit;

import com.vitatrack.utils.BMIUtil;
import com.vitatrack.utils.CalorieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Boundary Value Analysis – TestPlan mục 8.2
 * Kiểm tra toàn bộ bảng BVA: Chiều cao, Cân nặng, Tuổi, Calo, Thời lượng vận động
 */
@DisplayName("BVA – Boundary Value Analysis (TestPlan mục 8.2)")
class BoundaryValueTest {

    private BMIUtil bmiUtil;
    private CalorieUtil calorieUtil;

    @BeforeEach
    void setUp() {
        bmiUtil = new BMIUtil();
        calorieUtil = new CalorieUtil();
    }

    // ── Chiều cao (cm): valid 50–250 ───────────────────────────────────────
    @ParameterizedTest(name = "height={0}cm → valid={1}")
    @CsvSource({
        "50,  true",   // biên dưới hợp lệ
        "51,  true",   // trong biên
        "249, true",   // trong biên
        "250, true",   // biên trên hợp lệ
    })
    @DisplayName("TC-M02-002/004 | Chiều cao hợp lệ → BMI > 0")
    void height_valid_range(int heightCm, boolean valid) {
        double bmi = bmiUtil.calculateBMI(70, heightCm);
        if (valid) assertThat(bmi).isGreaterThan(0);
    }

    // ── Cân nặng (kg): valid 20–300 ───────────────────────────────────────
    @ParameterizedTest(name = "weight={0}kg → BMI > 0")
    @CsvSource({"20", "21", "299", "300"})
    @DisplayName("TC-M02-BVA | Cân nặng hợp lệ (20–300kg) → BMI > 0")
    void weight_valid_range(double weightKg) {
        double bmi = bmiUtil.calculateBMI(weightKg, 170);
        assertThat(bmi).isGreaterThan(0);
    }

    // ── Tuổi: valid 1–120 ─────────────────────────────────────────────────
    @ParameterizedTest(name = "age={0} → BMR > 0")
    @CsvSource({"1", "2", "119", "120"})
    @DisplayName("TC-M02-BVA | Tuổi hợp lệ (1–120) → BMR > 0")
    void age_valid_range(int age) {
        double bmr = bmiUtil.calculateBMR(70, 175, age, "male");
        assertThat(bmr).isGreaterThan(0);
    }

    // ── Calo thực phẩm (/100g): valid 0–9999 ─────────────────────────────
    @ParameterizedTest(name = "cal={0}/100g qty=100g → caloriesConsumed={1}")
    @CsvSource({
        "0,    0.0",
        "1,    1.0",
        "9998, 9998.0",
        "9999, 9999.0"
    })
    @DisplayName("TC-M03-BVA | Calo thực phẩm biên hợp lệ → tính đúng")
    void food_calories_valid_boundary(double calPer100g, double expected) {
        double cal = calorieUtil.calcNutrient(calPer100g, 100);
        assertThat(cal).isCloseTo(expected, within(0.1));
    }

    // ── Thời lượng vận động (phút): valid 1–600 ───────────────────────────
    @ParameterizedTest(name = "duration={0}min → cal > 0")
    @CsvSource({"1", "2", "599", "600"})
    @DisplayName("TC-M04-BVA | Thời lượng vận động hợp lệ (1–600 phút) → calo > 0")
    void activity_duration_valid(int minutes) {
        double cal = calorieUtil.estimateCaloriesBurned("running", minutes, 70);
        assertThat(cal).isGreaterThan(0);
    }

    // ── Thời lượng biên không hợp lệ ─────────────────────────────────────
    @ParameterizedTest(name = "duration={0}min → 0 kcal (biên không hợp lệ)")
    @CsvSource({"0"})
    @DisplayName("TC-M04-005 | BVA: Thời lượng = 0 → 0 kcal")
    void activity_duration_zero_boundary(int minutes) {
        double cal = calorieUtil.estimateCaloriesBurned("running", minutes, 70);
        assertThat(cal).isEqualTo(0.0);
    }
}
