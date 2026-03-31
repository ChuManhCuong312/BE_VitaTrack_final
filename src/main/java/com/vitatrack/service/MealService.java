package com.vitatrack.service;

import com.vitatrack.dto.meal.AddMealDTO;
import com.vitatrack.dto.meal.FoodSearchDTO;
import com.vitatrack.dto.meal.MealResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MealService {
    List<MealResponse> getDiary(Long userId, LocalDate date);
    MealResponse addFoodEntry(Long userId, AddMealDTO dto);
    void deleteFoodEntry(Long userId, Long entryId);
    Page<FoodSearchDTO> searchFood(String query, int page, int size);
    FoodSearchDTO getFoodDetail(Long foodId);
    FoodSearchDTO addCustomFood(Long userId, java.util.Map<String, Object> data);
    Map<String, Object> recognizeFood(MultipartFile image);
    Map<String, Object> getStats(Long userId, String period);
}
