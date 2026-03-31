package com.vitatrack.utils;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MET-based calorie burn estimation.
 * Reference: Ainsworth et al., Compendium of Physical Activities (2011)
 */
@Component
public class CalorieUtil {

    /** MET values by activity type (approximate) */
    private static final Map<String, Double> MET_VALUES = Map.ofEntries(
        Map.entry("walking",     3.5),
        Map.entry("running",     8.0),
        Map.entry("cycling",     6.0),
        Map.entry("swimming",    6.0),
        Map.entry("gym",         5.0),
        Map.entry("yoga",        3.0),
        Map.entry("dancing",     4.5),
        Map.entry("hiking",      5.3),
        Map.entry("basketball",  6.5),
        Map.entry("football",    7.0),
        Map.entry("badminton",   5.5),
        Map.entry("tennis",      7.3),
        Map.entry("other",       4.0)
    );

    /**
     * Estimate calories burned.
     * Formula: Calories = MET × weightKg × durationHours
     */
    public double estimateCaloriesBurned(String activityType, int durationMinutes, double weightKg) {
        double met = MET_VALUES.getOrDefault(
                activityType == null ? "other" : activityType.toLowerCase(), 4.0);
        double hours = durationMinutes / 60.0;
        return Math.round(met * weightKg * hours * 10.0) / 10.0;
    }

    /**
     * Calculate macros for a given quantity (g) from per-100g values.
     */
    public double calcNutrient(double per100g, double quantityG) {
        return Math.round((per100g * quantityG / 100.0) * 10.0) / 10.0;
    }
}
