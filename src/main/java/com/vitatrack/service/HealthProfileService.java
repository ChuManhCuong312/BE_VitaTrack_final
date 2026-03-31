package com.vitatrack.service;

import com.vitatrack.dto.health.HealthProfileDTO;
import com.vitatrack.entity.Allergy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HealthProfileService – interface đầy đủ theo SRS FR-06 → FR-11.
 */
public interface HealthProfileService {

    // FR-06 / FR-09
    HealthProfileDTO getMetrics(Long userId);
    HealthProfileDTO saveMetrics(Long userId, Map<String, Object> data);
    HealthProfileDTO getMetricsHistory(Long userId, String period);

    // FR-07
    HealthProfileDTO getGoals(Long userId);
    HealthProfileDTO updateGoals(Long userId, Map<String, Object> goals);

    // FR-10 Weight History
    HealthProfileDTO addWeightRecord(Long userId, double weight, String note, LocalDate date);
    void deleteWeightRecord(Long userId, Long recordId);

    // FR-11 Allergy CRUD
    Allergy addAllergy(Long userId, String allergenName, String severity, String note);
    List<Allergy> getAllergies(Long userId);
    void deleteAllergy(Long userId, Long allergyId);
    Allergy updateAllergy(Long userId, Long allergyId, String severity, String note);
}
