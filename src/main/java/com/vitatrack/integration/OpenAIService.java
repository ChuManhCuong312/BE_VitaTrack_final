package com.vitatrack.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitatrack.dto.ai.FoodImageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OpenAIService – tương thích với bất kỳ provider OpenAI-compatible:
 *   - Groq     : base-url=https://api.groq.com/openai/v1
 *   - OpenAI   : base-url=https://api.openai.com/v1
 *   - Gemini   : base-url=https://generativelanguage.googleapis.com/v1beta/openai
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.vision-model}")
    private String visionModel;

    @Value("${openai.max-tokens}")
    private int maxTokens;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private WebClient getClient() {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── Chat completion (OpenAI-compatible) ───────────────────────────────
    public String chatCompletion(String systemPrompt, List<Map<String, String>> messages) {
        try {
            List<Map<String, String>> allMessages = new java.util.ArrayList<>();
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
            allMessages.addAll(messages);

            Map<String, Object> body = Map.of(
                    "model",      model,
                    "messages",   allMessages,
                    "max_tokens", maxTokens
            );

            String response = getClient().post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(err -> {
                                log.error("[AI] API error status={} body={}", resp.statusCode(), err);
                                return Mono.error(new RuntimeException("API error: " + err));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("[AI] chatCompletion error: {}", e.getMessage());
            return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại sau.";
        }
    }

    // ── Vision / food image analysis ──────────────────────────────────────
    public FoodImageRequest analyzeFoodImage(byte[] imageBytes, String mimeType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String dataUrl     = "data:" + mimeType + ";base64," + base64Image;

            String prompt = """
                Analyze this food image and respond ONLY with a JSON object (no markdown):
                {
                  "detectedFoodName": "...",
                  "estimatedWeight": 150,
                  "estimatedCalories": 320,
                  "estimatedProtein": 12.5,
                  "estimatedCarbs": 45.0,
                  "estimatedFat": 8.0,
                  "confidence": 0.85,
                  "rawAiDescription": "Brief description"
                }
                Weights in grams, calories in kcal, macros in grams per estimated portion.
                """;

            Map<String, Object> body = Map.of(
                    "model",      visionModel,
                    "max_tokens", 500,
                    "messages",   List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", prompt),
                                    Map.of("type", "image_url",
                                            "image_url", Map.of("url", dataUrl, "detail", "low"))
                            )
                    ))
            );

            String response = getClient().post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class).flatMap(err -> {
                                log.error("[Vision] API error status={} body={}", resp.statusCode(), err);
                                return Mono.error(new RuntimeException("Vision API error: " + err));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            JsonNode root    = objectMapper.readTree(response);
            String   content = root.path("choices").get(0).path("message").path("content").asText();
            content = content.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(content, FoodImageRequest.class);

        } catch (Exception e) {
            log.error("[Vision] analyzeFoodImage error: {}", e.getMessage());
            return FoodImageRequest.builder()
                    .detectedFoodName("Không xác định")
                    .estimatedCalories(0.0)
                    .confidence(0.0)
                    .rawAiDescription("Không thể phân tích ảnh")
                    .build();
        }
    }

    // ── Meal plan generation ──────────────────────────────────────────────
    public String generateMealPlanSuggestion(Map<String, Object> preferences) {
        String systemPrompt = """
                Bạn là chuyên gia dinh dưỡng VitaTrack.
                Tạo gợi ý thực đơn ngắn gọn, phù hợp người Việt Nam.
                Trả lời bằng tiếng Việt.
                """;
        String userMsg = "Tạo gợi ý thực đơn với thông tin: " + preferences.toString();
        return chatCompletion(systemPrompt, List.of(Map.of("role", "user", "content", userMsg)));
    }
}