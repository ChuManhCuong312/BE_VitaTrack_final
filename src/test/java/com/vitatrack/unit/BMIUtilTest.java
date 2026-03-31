package com.vitatrack.unit;

import com.vitatrack.utils.BMIUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests cho BMIUtil
 * Test Plan ref: TC-M02-001, TC-M02-008 – BMI/BMR/TDEE/Phân loại
 * Bảng kết quả mong đợi theo TestPlan mục 8.3
 */
@DisplayName("BMIUtil – Tính toán BMI, BMR, TDEE")
class BMIUtilTest {

    private BMIUtil bmiUtil;

    @BeforeEach
    void setUp() { bmiUtil = new BMIUtil(); }

    @Nested
    @DisplayName("calculateBMI")
    class CalculateBMI {

        @Test
        @DisplayName("TC-M02-001 | Nam 175cm 70kg → BMI = 22.9")
        void bmi_normal_male() {
            assertThat(bmiUtil.calculateBMI(70, 175)).isEqualTo(22.9);
        }

        @Test
        @DisplayName("TC-M02-001 | Nữ 160cm 80kg → BMI = 31.3 (Béo phì)")
        void bmi_obese_female() {
            assertThat(bmiUtil.calculateBMI(80, 160)).isEqualTo(31.3);
        }

        @Test
        @DisplayName("TC-M02-001 | Nam 170cm 50kg → BMI = 17.3 (Thiếu cân)")
        void bmi_underweight_male() {
            assertThat(bmiUtil.calculateBMI(50, 170)).isEqualTo(17.3);
        }

        @Test
        @DisplayName("TC-M02-001 | Nữ 155cm 45kg → BMI ≈ 18.7")
        void bmi_normal_low_female() {
            assertThat(bmiUtil.calculateBMI(45, 155)).isCloseTo(18.7, within(0.1));
        }

        @Test
        @DisplayName("TC-M02-002 | BVA: Chiều cao = 50cm (biên dưới hợp lệ)")
        void bmi_height_min_valid() {
            assertThat(bmiUtil.calculateBMI(20, 50)).isGreaterThan(0);
        }

        @Test
        @DisplayName("TC-M02-004 | BVA: Chiều cao = 250cm (biên trên hợp lệ)")
        void bmi_height_max_valid() {
            assertThat(bmiUtil.calculateBMI(100, 250)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("calculateBMR – Mifflin-St Jeor")
    class CalculateBMR {

        @Test
        @DisplayName("TC-M02-001 | Nam 25t 175cm 70kg → BMR ≈ 1674 kcal")
        void bmr_male_25yo() {
            // 10×70 + 6.25×175 − 5×25 + 5 = 1673.75
            assertThat(bmiUtil.calculateBMR(70, 175, 25, "male")).isCloseTo(1674, within(5.0));
        }

        @Test
        @DisplayName("TC-M02-001 | Nữ 30t 160cm 80kg → BMR ≈ 1489 kcal")
        void bmr_female_30yo() {
            // 10×80 + 6.25×160 − 5×30 − 161 = 1489
            assertThat(bmiUtil.calculateBMR(80, 160, 30, "female")).isCloseTo(1489, within(5.0));
        }

        @Test
        @DisplayName("TC-M02-001 | Nam 18t 170cm 50kg → BMR ≈ 1478 kcal")
        void bmr_male_18yo() {
            assertThat(bmiUtil.calculateBMR(50, 170, 18, "male")).isCloseTo(1478, within(5.0));
        }

        @Test
        @DisplayName("TC-M02-001 | Nữ 45t 155cm 45kg → BMR ≈ 1033 kcal")
        void bmr_female_45yo() {
            assertThat(bmiUtil.calculateBMR(45, 155, 45, "female")).isCloseTo(1033, within(5.0));
        }

        @Test
        @DisplayName("BMR | Gender không phân biệt hoa thường")
        void bmr_case_insensitive() {
            assertThat(bmiUtil.calculateBMR(70, 175, 25, "MALE"))
                .isEqualTo(bmiUtil.calculateBMR(70, 175, 25, "male"));
        }
    }

    @Nested
    @DisplayName("calculateTDEE")
    class CalculateTDEE {

        @ParameterizedTest(name = "{0} → ×{1}")
        @CsvSource({
            "sedentary,   1.2",
            "light,       1.375",
            "moderate,    1.55",
            "active,      1.725",
            "very_active, 1.9"
        })
        @DisplayName("TC-M02-009 | TDEE – kiểm tra tất cả mức độ vận động")
        void tdee_all_levels(String level, double multiplier) {
            assertThat(bmiUtil.calculateTDEE(1600, level)).isCloseTo(1600 * multiplier, within(2.0));
        }

        @Test
        @DisplayName("TDEE | null activity → mặc định sedentary (×1.2)")
        void tdee_null_defaults_sedentary() {
            assertThat(bmiUtil.calculateTDEE(1600, null)).isCloseTo(1920, within(2.0));
        }
    }

    @Nested
    @DisplayName("getBMICategory – TC-M02-008")
    class BMICategory {

        @ParameterizedTest(name = "BMI={0} → {1}")
        @CsvSource({
            "17.3, Thiếu cân",
            "18.4, Thiếu cân",
            "18.5, Bình thường",
            "22.9, Bình thường",
            "24.9, Bình thường",
            "25.0, Thừa cân",
            "29.9, Thừa cân",
            "30.0, Béo phì",
            "31.3, Béo phì"
        })
        @DisplayName("Phân loại theo WHO standard")
        void categories(double bmi, String expected) {
            assertThat(bmiUtil.getBMICategory(bmi)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("calculateDailyCalorieGoal – TC-M02-009")
    class DailyCalorieGoal {

        @Test @DisplayName("Giảm cân → TDEE − 500")
        void lose_weight() { assertThat(bmiUtil.calculateDailyCalorieGoal(2000, "LOSE_WEIGHT")).isEqualTo(1500); }

        @Test @DisplayName("Tăng cân → TDEE + 500")
        void gain_muscle() { assertThat(bmiUtil.calculateDailyCalorieGoal(2000, "GAIN_MUSCLE")).isEqualTo(2500); }

        @Test @DisplayName("Duy trì → TDEE")
        void maintain() { assertThat(bmiUtil.calculateDailyCalorieGoal(2000, "MAINTAIN")).isEqualTo(2000); }

        @Test @DisplayName("Safety floor: không giảm dưới 1200 kcal")
        void minimum_1200() { assertThat(bmiUtil.calculateDailyCalorieGoal(1500, "LOSE_WEIGHT")).isGreaterThanOrEqualTo(1200); }
    }
}
