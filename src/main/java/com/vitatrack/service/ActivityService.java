package com.vitatrack.service;

import com.vitatrack.dto.activity.ActivityDTO;
import com.vitatrack.dto.activity.ActivityResponse;

import java.util.List;
import java.util.Map;

public interface ActivityService {
    List<ActivityResponse> getToday(Long userId);
    ActivityResponse addActivity(Long userId, ActivityDTO dto);
    void deleteActivity(Long userId, Long activityId);
    List<ActivityResponse> getHistory(Long userId, String from, String to);
    Map<String, Object> syncWearable(Long userId, String device, String accessToken);
    Map<String, Object> getWearableStatus(Long userId);
    Map<String, Object> getStats(Long userId, String period);
}
