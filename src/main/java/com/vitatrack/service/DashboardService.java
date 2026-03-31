package com.vitatrack.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getDashboard(Long userId);
}
