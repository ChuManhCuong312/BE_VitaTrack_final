package com.vitatrack.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for adding an activity log entry.
 * Bổ sung sleepHours, heartRateAvg, heartRateMax để hỗ trợ
 * Alert Engine (FR-09): SLEEP_DEFICIT, TACHYCARDIA, BRADYCARDIA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {
    private String type;
    private Integer duration;
    private Double calories;
    private Double distance;
    private Integer stepsCount;
    private LocalDate date;
    private String notes;

    /** Giờ ngủ – trigger SLEEP_DEFICIT nếu < 6h */
    private Double sleepHours;

    /** Nhịp tim TB (bpm) – trigger TACHYCARDIA >100 hoặc BRADYCARDIA <50 */
    private Integer heartRateAvg;

    /** Nhịp tim tối đa */
    private Integer heartRateMax;
}
