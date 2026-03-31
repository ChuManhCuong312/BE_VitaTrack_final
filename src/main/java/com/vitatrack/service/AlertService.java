package com.vitatrack.service;

import com.vitatrack.entity.HealthAlert;
import java.util.List;

public interface AlertService {
    List<HealthAlert> getAlerts(Long userId);
    List<HealthAlert> getUnread(Long userId);
    long countUnread(Long userId);
    HealthAlert markRead(Long userId, Long alertId);
    void markAllRead(Long userId);
    void evaluate(Long userId);
    /** FR-32.Acknowledge – Expert đánh dấu đã xử lý + ghi note */
    void acknowledge(Long expertUserId, Long alertId, String note);
}
