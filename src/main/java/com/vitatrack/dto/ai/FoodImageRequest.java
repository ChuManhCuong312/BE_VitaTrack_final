package com.vitatrack.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Response from food image analysis */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FoodImageRequest {
    // Used as response too
    private String detectedFoodName;
    private Double estimatedCalories;
    private Double estimatedProtein;
    private Double estimatedCarbs;
    private Double estimatedFat;
    private Double estimatedWeight;   // grams
    private Double confidence;        // 0.0 – 1.0
    private List<Long> matchedFoodIds; // DB food IDs that match
    private String rawAiDescription;
}
