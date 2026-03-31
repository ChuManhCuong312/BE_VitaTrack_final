package com.vitatrack.service;

import java.util.List;
import java.util.Map;

/**
 * AIService – interface đầy đủ theo SRS FR-24 → FR-27.
 */
public interface AIService {

    // FR-24 Chat
    Map<String, Object> sendMessage(Long userId, String message, String sessionId);

    // FR-24.MaxHistory / FR-27 History
    List<Map<String, Object>> getChatHistory(Long userId, String sessionId);

    // FR-27.Search
    List<Map<String, Object>> searchChatHistory(Long userId, String keyword);

    // FR-27.Delete
    void deleteChatSession(Long userId, String sessionId);
    void deleteAllChatHistory(Long userId);

    // FR-27.SessionGroup – danh sách session
    List<Map<String, Object>> getConversations(Long userId);

    // Legacy – giữ tương thích
    void deleteConversation(Long userId, String conversationId);

    // FR-26 Meal Suggest
    Map<String, Object> generateMealPlan(Long userId, Map<String, Object> preferences);

    // Dashboard suggestions
    List<Map<String, Object>> getDailySuggestions(Long userId);
}
