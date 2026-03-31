package com.vitatrack.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMealDTO {
    private Long foodId;
    private String mealType;
    private LocalDate date;
    private Double quantity;
    private String unit;
    private boolean addedViaAi;
}
