package com.vitatrack.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DateUtil {

    /** Convert period string (7d, 30d, 3m, 6m, 1y) to start date */
    public LocalDate periodToStartDate(String period) {
        LocalDate today = LocalDate.now();
        return switch (period == null ? "7d" : period.toLowerCase()) {
            case "30d" -> today.minusDays(30);
            case "3m"  -> today.minusMonths(3);
            case "6m"  -> today.minusMonths(6);
            case "1y"  -> today.minusYears(1);
            default    -> today.minusDays(7);   // "7d"
        };
    }
}
