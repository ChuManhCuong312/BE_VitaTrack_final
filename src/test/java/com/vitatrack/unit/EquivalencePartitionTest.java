package com.vitatrack.unit;

import com.vitatrack.utils.BMIUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Equivalence Partitioning – TestPlan mục 5.2
 * Phân lớp BMI: Thiếu cân / Bình thường / Thừa cân / Béo phì
 * Phân lớp calo: hợp lệ (≥0) / không hợp lệ (<0)
 */
@DisplayName("EP – Equivalence Partitioning")
class EquivalencePartitionTest {

    private BMIUtil bmiUtil;

    @BeforeEach
    void setUp() { bmiUtil = new BMIUtil(); }

    // ── Phân lớp BMI (TC-M02-008) ─────────────────────────────────────────
    @ParameterizedTest(name = "BMI={0} → Lớp: {1}")
    @CsvSource({
        // Lớp 1: Thiếu cân  (BMI < 18.5)
        "15.0, Thiếu cân",
        "17.3, Thiếu cân",
        "18.4, Thiếu cân",
        // Lớp 2: Bình thường (18.5 ≤ BMI < 25.0)
        "18.5, Bình thường",
        "22.0, Bình thường",
        "24.9, Bình thường",
        // Lớp 3: Thừa cân    (25.0 ≤ BMI < 30.0)
        "25.0, Thừa cân",
        "27.5, Thừa cân",
        "29.9, Thừa cân",
        // Lớp 4: Béo phì     (BMI ≥ 30.0)
        "30.0, Béo phì",
        "31.3, Béo phì",
        "40.0, Béo phì"
    })
    @DisplayName("TC-M02-008 | EP: Phân lớp BMI theo WHO")
    void bmi_equivalence_classes(double bmi, String expectedCategory) {
        assertThat(bmiUtil.getBMICategory(bmi)).isEqualTo(expectedCategory);
    }

    // ── Phân lớp activity level cho TDEE ──────────────────────────────────
    @ParameterizedTest(name = "activityLevel='{0}' → TDEE={2}×BMR")
    @CsvSource({
        // Lớp SEDENTARY
        "sedentary,   1600, 1920",
        // Lớp LIGHT
        "light,       1600, 2200",
        // Lớp MODERATE
        "moderate,    1600, 2480",
        // Lớp ACTIVE
        "active,      1600, 2760",
        // Lớp VERY_ACTIVE
        "very_active, 1600, 3040"
    })
    @DisplayName("EP | TDEE – Phân lớp mức độ vận động")
    void tdee_activity_levels(String level, double bmr, double expectedApprox) {
        double tdee = bmiUtil.calculateTDEE(bmr, level);
        assertThat(tdee).isBetween(expectedApprox - 50, expectedApprox + 50);
    }
}
