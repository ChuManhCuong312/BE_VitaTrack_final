package com.vitatrack.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BMIResponse {
    private Double bmi;
    private String category;    // Thiếu cân / Bình thường / Thừa cân / Béo phì
    private Double bmr;
    private Double tdee;
    private Integer recommendedDailyCalories;
}
