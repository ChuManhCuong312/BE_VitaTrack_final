package com.vitatrack.service;

import java.util.List;
import java.util.Map;

public interface ExpertService {
    List<Map<String, Object>> getClients(Long expertId);
    List<Map<String, Object>> getChatHistory(Long expertId, Long clientId);
    Map<String, Object> sendMessage(Long expertId, Long clientId, String content);
    List<Map<String, Object>> getMealPlans(Long expertId);
    Map<String, Object> createMealPlan(Long expertId, Map<String, Object> data);
    Map<String, Object> sendMealPlan(Long expertId, Long planId);
    Map<String, Object> getClientDashboard(Long expertId, Long clientId);
}
