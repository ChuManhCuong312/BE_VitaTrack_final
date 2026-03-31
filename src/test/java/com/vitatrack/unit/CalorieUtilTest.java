package com.vitatrack.unit;

import com.vitatrack.utils.CalorieUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests cho CalorieUtil
 * Test Plan ref: TC-M04-004, TC-M04-005 – Tính calo tiêu thụ & dinh dưỡng
 */
@DisplayName("CalorieUtil – Tính calo & macro")
class CalorieUtilTest {

    private CalorieUtil calorieUtil;

    @BeforeEach
    void setUp() { calorieUtil = new CalorieUtil(); }

    @Nested
    @DisplayName("estimateCaloriesBurned – TC-M04-004")
    class EstimateCalories {

        @Test
        @DisplayName("TC-M04-004 | Chạy bộ 30 phút, 70kg → ≈ 280 kcal (MET=8.0)")
        void running_30min_70kg() {
            // MET(running)=8.0 × 70 × 0.5h = 280
            double cal = calorieUtil.estimateCaloriesBurned("running", 30, 70);
            assertThat(cal).isCloseTo(280, within(5.0));
        }

        @Test
        @DisplayName("TC-M04-004 | Đi bộ 60 phút, 65kg → ≈ 227.5 kcal (MET=3.5)")
        void walking_60min_65kg() {
            double cal = calorieUtil.estimateCaloriesBurned("walking", 60, 65);
            assertThat(cal).isCloseTo(227.5, within(5.0));
        }

        @Test
        @DisplayName("TC-M04-004 | Đạp xe 45 phút, 80kg → ≈ 360 kcal (MET=6.0)")
        void cycling_45min_80kg() {
            double cal = calorieUtil.estimateCaloriesBurned("cycling", 45, 80);
            assertThat(cal).isCloseTo(360, within(5.0));
        }

        @Test
        @DisplayName("TC-M04-005 | Thời lượng = 0 phút → 0 kcal")
        void zero_duration_returns_zero() {
            double cal = calorieUtil.estimateCaloriesBurned("running", 0, 70);
            assertThat(cal).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Activity không có trong danh sách → dùng MET mặc định (4.0)")
        void unknown_activity_default_met() {
            double cal = calorieUtil.estimateCaloriesBurned("archery", 60, 70);
            // MET=4.0 × 70 × 1h = 280
            assertThat(cal).isCloseTo(280, within(5.0));
        }

        @Test
        @DisplayName("Activity = null → không crash, dùng default MET")
        void null_activity_no_crash() {
            assertThatCode(() -> calorieUtil.estimateCaloriesBurned(null, 30, 70))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0} → MET ≥ 0")
        @CsvSource({"walking", "running", "cycling", "swimming", "gym", "yoga", "dancing",
                    "hiking", "basketball", "football", "badminton", "tennis"})
        @DisplayName("Tất cả activity type được định nghĩa → calo > 0")
        void all_known_activities_positive(String activity) {
            double cal = calorieUtil.estimateCaloriesBurned(activity, 30, 70);
            assertThat(cal).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("calcNutrient – TC-M03-001")
    class CalcNutrient {

        @Test
        @DisplayName("TC-M03-001 | Cơm trắng 200g: protein = 2.7/100g × 200g = 5.4g")
        void protein_rice_200g() {
            assertThat(calorieUtil.calcNutrient(2.7, 200)).isEqualTo(5.4);
        }

        @Test
        @DisplayName("TC-M03-001 | Cơm trắng 200g: calo = 130/100g × 200g = 260 kcal")
        void calories_rice_200g() {
            assertThat(calorieUtil.calcNutrient(130, 200)).isEqualTo(260.0);
        }

        @Test
        @DisplayName("Khẩu phần = 0g → trả về 0")
        void zero_quantity_returns_zero() {
            assertThat(calorieUtil.calcNutrient(130, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("TC-M03-005 | Sửa 100g → 200g: giá trị phải nhân đôi")
        void double_quantity_doubles_nutrient() {
            double before = calorieUtil.calcNutrient(130, 100);
            double after  = calorieUtil.calcNutrient(130, 200);
            assertThat(after).isEqualTo(before * 2);
        }
    }
}
