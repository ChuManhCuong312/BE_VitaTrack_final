package com.vitatrack.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private String activityType;
    private Integer duration;
    private Double caloriesBurned;
    private Double distance;
    private Integer stepsCount;
    private Double sleepHours;
    private Integer heartRateAvg;
    private Integer heartRateMax;
    private LocalDate date;
    private String notes;
    private String source;
    private LocalDateTime createdAt;
}
