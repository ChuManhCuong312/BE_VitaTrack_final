package com.vitatrack.service.impl;

import com.vitatrack.entity.ChatMessage;
import com.vitatrack.entity.HealthProfile;
import com.vitatrack.entity.User;
import com.vitatrack.exception.VitaTrackException;
import com.vitatrack.integration.OpenAIService;
import com.vitatrack.repository.ActivityRepository;
import com.vitatrack.repository.ChatMessageRepository;
import com.vitatrack.repository.HealthProfileRepository;
import com.vitatrack.repository.MealRepository;
import com.vitatrack.repository.UserRepository;
import com.vitatrack.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;


/**
 * AIServiceImpl – cập nhật theo SRS FR-24 → FR-27:
 *
 *  FR-24.LLMTimeout    15 giây timeout tuyệt đối; trả LlmTimeoutException
 *  FR-24.Disclaimer    Kèm disclaimer ⚠ cho mọi phản hồi tư vấn sức khỏe
 *  FR-24.Languages     Tự động nhận biết ngôn ngữ (Vietnamese/English)
 *  FR-24.MaxHistory    Lazy load 50 tin nhắn/lần
 *  FR-25.SharedContext Truyền thông tin sức khỏe ẩn danh nếu user đồng ý
 *  FR-25.DataPrivacy   Tuyệt đối không truyền tên, email vào LLM
 *  FR-27.AutoSave      Lưu toàn bộ hội thoại vào bảng chat_messages
 *  FR-27.SessionGroup  Session mới sau 30 phút không hoạt động
 *  FR-27.Search        Tìm kiếm trong lịch sử
 *  FR-27.Delete        Xóa session hoặc toàn bộ lịch sử
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AIServiceImpl implements AIService {

    private static final int    LLM_TIMEOUT_SECONDS   = 15;   // FR-24.LLMTimeout
    private static final int    SESSION_IDLE_MINUTES   = 30;   // FR-27.SessionGroup
    private static final int    MAX_HISTORY_PER_PAGE   = 50;   // FR-24.MaxHistory
    private static final String DISCLAIMER = "\n\n⚠ Đây là gợi ý tham khảo, không thay thế lời khuyên của chuyên gia y tế hoặc dinh dưỡng.";

    // Từ khóa cần kèm disclaimer (FR-24.Disclaimer)
    private static final List<String> HEALTH_KEYWORDS = List.of(
        "calo", "calorie", "dinh dưỡng", "bmi", "cân nặng", "giảm cân", "tăng cân",
        "protein", "carbs", "fat", "chất béo", "vitamin", "nutrition", "weight",
        "diet", "meal", "exercise", "workout", "health"
    );

    private final OpenAIService          openAIService;
    private final HealthProfileRepository healthProfileRepository;
    private final MealRepository         mealRepository;
    private final ActivityRepository     activityRepository;
    private final UserRepository         userRepository;
    private final ChatMessageRepository  chatMessageRepository;

    // ── FR-24 sendMessage ─────────────────────────────────────────────────

    @Override
    public Map<String, Object> sendMessage(Long userId, String message, String sessionId) {
        // FR-27.SessionGroup – tạo session mới nếu cần
        String activeSession = resolveSession(userId, sessionId);

        // FR-27.AutoSave – lưu tin nhắn user
        saveMessage(userId, activeSession, "user", message, false);

        // Lấy lịch sử gần nhất để gửi vào LLM context
        List<ChatMessage> history = chatMessageRepository
                .findBySessionIdOrderBySentAt(activeSession);
        List<Map<String, String>> llmHistory = history.stream()
                .map(m -> Map.of("role", m.getSender().equals("user") ? "user" : "assistant",
                                  "content", m.getContent()))
                .toList();

        String systemPrompt = buildSystemPrompt(userId);

        // FR-24.LLMTimeout – 15 giây timeout
        String reply = callLlmWithTimeout(systemPrompt, new ArrayList<>(llmHistory));

        // FR-24.Disclaimer – kèm disclaimer nếu phản hồi liên quan sức khỏe
        boolean needsDisclaimer = isHealthRelated(message) || isHealthRelated(reply);
        String finalReply = needsDisclaimer ? reply + DISCLAIMER : reply;

        // FR-27.AutoSave – lưu phản hồi AI
        saveMessage(userId, activeSession, "ai", finalReply, needsDisclaimer);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId",      activeSession);
        response.put("reply",          finalReply);
        response.put("role",           "assistant");
        response.put("hasDisclaimer",  needsDisclaimer);
        response.put("timestamp",      LocalDateTime.now().toString());
        return response;
    }

    // ── FR-24.MaxHistory / FR-27 getChatHistory ───────────────────────────

    @Override
    public List<Map<String, Object>> getChatHistory(Long userId, String sessionId) {
        List<ChatMessage> messages;
        if (sessionId != null) {
            messages = chatMessageRepository.findBySessionIdOrderBySentAt(sessionId);
        } else {
            // FR-24.MaxHistory – 50 tin nhắn gần nhất
            messages = chatMessageRepository.findLatestByUserId(userId,
                    PageRequest.of(0, MAX_HISTORY_PER_PAGE));
            Collections.reverse(messages);
        }

        return messages.stream().map(m -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",             m.getId());
            item.put("sessionId",      m.getSessionId());
            item.put("sender",         m.getSender());
            item.put("content",        m.getContent());
            item.put("hasDisclaimer",  m.isHasDisclaimer());
            item.put("sentAt",         m.getSentAt().toString());
            return item;
        }).toList();
    }

    // ── FR-27.Search ─────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> searchChatHistory(Long userId, String keyword) {
        return chatMessageRepository.searchByContent(userId, keyword)
                .stream().map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id",        m.getId());
                    item.put("sessionId", m.getSessionId());
                    item.put("sender",    m.getSender());
                    item.put("content",   m.getContent());
                    item.put("sentAt",    m.getSentAt().toString());
                    return item;
                }).toList();
    }

    // ── FR-27.Delete ──────────────────────────────────────────────────────

    @Override
    public void deleteChatSession(Long userId, String sessionId) {
        chatMessageRepository.deleteBySessionIdAndUserId(sessionId, userId);
        log.info("[AI] Deleted chat session={} for userId={}", sessionId, userId);
    }

    @Override
    public void deleteAllChatHistory(Long userId) {
        chatMessageRepository.deleteAllByUserId(userId);
        log.info("[AI] Deleted all chat history for userId={}", userId);
    }

    // ── FR-26 generateMealPlan ────────────────────────────────────────────

    @Override
    public Map<String, Object> generateMealPlan(Long userId, Map<String, Object> preferences) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);

        Map<String, Object> contextPrefs = new LinkedHashMap<>(preferences);
        if (profile != null) {
            contextPrefs.put("dailyCalorieGoal", profile.getDailyCalorieGoal());
            contextPrefs.put("goalType",         profile.getGoalType());
            // FR-25.DataPrivacy – không truyền thông tin nhận dạng
            contextPrefs.put("allergies",        profile.getAllergies());
        }

        double caloriesConsumed = mealRepository.sumCaloriesByUserAndDate(userId, LocalDate.now());
        int goal = profile != null && profile.getDailyCalorieGoal() != null
                ? profile.getDailyCalorieGoal() : 2000;

        // FR-26.LowBudget / ExhaustedBudget
        double remaining = Math.max(0, goal - caloriesConsumed);
        contextPrefs.put("remainingCalories", remaining);

        String suggestion = openAIService.generateMealPlanSuggestion(contextPrefs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("suggestion",        suggestion + DISCLAIMER);
        result.put("remainingCalories", remaining);
        result.put("generatedAt",       LocalDateTime.now().toString());
        return result;
    }

    // ── getDailySuggestions ───────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getDailySuggestions(Long userId) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        double consumed = mealRepository.sumCaloriesByUserAndDate(userId, LocalDate.now());
        double burned   = activityRepository.sumCaloriesBurnedByUserAndDate(userId, LocalDate.now());
        int goal = profile != null && profile.getDailyCalorieGoal() != null
                ? profile.getDailyCalorieGoal() : 2000;

        List<Map<String, Object>> suggestions = new ArrayList<>();
        double remaining = goal - consumed;
        if (remaining > 0) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type",    "calorie_budget");
            s.put("message", String.format("Bạn còn %.0f kcal trong ngân sách hôm nay. Có thể ăn thêm một bữa nhẹ!", remaining));
            s.put("icon",    "🍽️");
            suggestions.add(s);
        } else if (remaining < -200) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("type",    "calorie_overage");
            s.put("message", String.format("Bạn đã vượt quá %.0f kcal so với mục tiêu. Hãy vận động thêm!", Math.abs(remaining)));
            s.put("icon",    "⚠️");
            suggestions.add(s);
        }

        if (profile != null && profile.getBmi() != null) {
            if (profile.getBmi() < 18.5) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type",    "bmi_alert");
                s.put("message", "BMI của bạn cho thấy bạn đang thiếu cân. Hãy tăng khẩu phần ăn và tư vấn chuyên gia.");
                s.put("icon",    "💡");
                suggestions.add(s);
            } else if (profile.getBmi() >= 30.0) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("type",    "bmi_alert");
                s.put("message", "BMI của bạn ở mức béo phì. Hãy lập kế hoạch dinh dưỡng với chuyên gia.");
                s.put("icon",    "💡");
                suggestions.add(s);
            }
        }

        return suggestions;
    }

    // ── FR-27.SessionGroup getConversations ───────────────────────────────

    @Override
    public List<Map<String, Object>> getConversations(Long userId) {
        List<String> sessionIds = chatMessageRepository.findDistinctSessionIdsByUserId(userId);
        return sessionIds.stream().map(sid -> {
            List<ChatMessage> msgs = chatMessageRepository.findBySessionIdOrderBySentAt(sid);
            String preview = msgs.isEmpty() ? "" : msgs.get(0).getContent();
            String lastTs  = msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).getSentAt().toString();
            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("sessionId",    sid);
            conv.put("messageCount", msgs.size());
            conv.put("preview",      preview.length() > 60 ? preview.substring(0, 60) + "…" : preview);
            conv.put("lastMessage",  lastTs);
            return conv;
        }).toList();
    }

    // ── Legacy deleteConversation (delegates to new method) ───────────────

    @Override
    public void deleteConversation(Long userId, String conversationId) {
        deleteChatSession(userId, conversationId);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * FR-27.SessionGroup – tìm session hiện tại hoặc tạo mới.
     * Session mới khi: sessionId null, hoặc khoảng nghỉ > 30 phút.
     */
    private String resolveSession(Long userId, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            // Kiểm tra session còn active (tin nhắn gần nhất < 30 phút)
            List<ChatMessage> existing = chatMessageRepository.findBySessionIdOrderBySentAt(sessionId);
            if (!existing.isEmpty()) {
                ChatMessage last = existing.get(existing.size() - 1);
                if (last.getSentAt().isAfter(LocalDateTime.now().minusMinutes(SESSION_IDLE_MINUTES))) {
                    return sessionId;
                }
            }
        }
        return UUID.randomUUID().toString();
    }

    /** FR-27.AutoSave – lưu một tin nhắn vào DB */
    private void saveMessage(Long userId, String sessionId, String sender,
                              String content, boolean hasDisclaimer) {
        User user = userRepository.findById(userId).orElseThrow();
        ChatMessage msg = ChatMessage.builder()
                .sessionId(sessionId)
                .user(user)
                .sender(sender)
                .content(content)
                .hasDisclaimer(hasDisclaimer)
                .build();
        chatMessageRepository.save(msg);
    }

    /**
     * FR-24.LLMTimeout – gọi OpenAI với timeout 15 giây.
     * Quá thời gian → LlmTimeoutException (504).
     */
    private String callLlmWithTimeout(String systemPrompt, List<Map<String, String>> history) {
        var future = CompletableFuture.supplyAsync(
                () -> openAIService.chatCompletion(systemPrompt, history));
        try {
            return future.get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("[AI] LLM timeout after {}s", LLM_TIMEOUT_SECONDS);
            throw new VitaTrackException.LlmTimeoutException();
        } catch (Exception e) {
            log.error("[AI] LLM error: {}", e.getMessage());
            throw new VitaTrackException.LlmTimeoutException();
        }
    }

    /**
     * FR-24.Disclaimer – kiểm tra tin nhắn có liên quan đến sức khỏe không.
     * Dùng lowercase match để tránh false negative.
     */
    private boolean isHealthRelated(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return HEALTH_KEYWORDS.stream().anyMatch(lower::contains);
    }

    /**
     * FR-25.SharedContext – xây system prompt với context sức khỏe ẩn danh.
     * FR-25.DataPrivacy – tuyệt đối không truyền tên, email.
     */
    private String buildSystemPrompt(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý ảo VitaTrack, chuyên về dinh dưỡng và sức khỏe. ");
        sb.append("Trả lời ngắn gọn, thực tế. Hỗ trợ tiếng Việt và tiếng Anh tự động. ");
        sb.append("Không cung cấp chẩn đoán y tế. Khuyên người dùng gặp chuyên gia khi cần. ");

        // FR-25.SharedContext – chỉ dữ liệu sức khỏe ẩn danh, không có PII
        healthProfileRepository.findByUserId(userId).ifPresent(p -> {
            if (p.getBmi() != null) {
                sb.append(String.format("Thông tin sức khỏe người dùng: BMI=%.1f (%s). ",
                        p.getBmi(), getBMICategory(p.getBmi())));
            }
            if (p.getGoalType() != null) {
                sb.append(String.format("Mục tiêu: %s. ", p.getGoalType()));
            }
            if (p.getDailyCalorieGoal() != null) {
                sb.append(String.format("Ngân sách calo: %d kcal/ngày. ", p.getDailyCalorieGoal()));
            }
        });
        return sb.toString();
    }

    private String getBMICategory(double bmi) {
        if (bmi < 18.5) return "thiếu cân";
        if (bmi < 25.0) return "bình thường";
        if (bmi < 30.0) return "thừa cân";
        return "béo phì";
    }
}
