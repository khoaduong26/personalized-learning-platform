package com.tlcn.backend.learningpath.generation;

import com.tlcn.backend.learningpath.generation.LearningPathOrderer.Candidate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnExpression("'${APP_LEARNING_PATH_GENERATOR_PROVIDER:rules}' == 'gemini' or "
        + "'${APP_LEARNING_PATH_GENERATOR_PROVIDER:rules}' == 'combined'")
public class GeminiLearningPathOrderer implements LearningPathOrderer, LearningPathOrderReviewer {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final JsonMapper mapper;

    @Autowired
    public GeminiLearningPathOrderer(@Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-3.8-flash}") String model, JsonMapper mapper) {
        this(apiKey, model, LearningPathHttpClientSupport.createClient(), mapper);
    }

    GeminiLearningPathOrderer(String apiKey, String model, RestClient client, JsonMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<Integer> order(List<Candidate> candidates) {
        String output = generateJson(LearningPathAiSupport.orderingPrompt(candidates),
                LearningPathAiSupport.geminiOrderSchema());
        return LearningPathAiSupport.parseOrder(output, mapper);
    }

    @Override
    public ReviewResult review(List<Candidate> candidates, List<Integer> orderedPositions) {
        String output = generateJson(LearningPathAiSupport.reviewPrompt(candidates, orderedPositions),
                LearningPathAiSupport.geminiReviewSchema());
        try {
            return mapper.readValue(output, ReviewResult.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Gemini trả về kết quả rà soát lộ trình không hợp lệ.", exception);
        }
    }

    private String generateJson(String prompt, Map<String, Object> schema) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY.");
        }
        Map<String, Object> body = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", prompt)})},
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", schema,
                        "temperature", 0.2,
                        "maxOutputTokens", 1000));
        JsonNode response = client.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return extractOutput(response);
    }

    private String extractOutput(JsonNode response) {
        JsonNode candidate = response == null ? null : response.path("candidates").path(0);
        if (candidate == null || candidate.isMissingNode()
                || !"STOP".equals(candidate.path("finishReason").asText())) {
            throw new IllegalStateException("Gemini không trả về kết quả lộ trình hoàn chỉnh.");
        }
        StringBuilder output = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("text").isTextual()) {
                output.append(part.path("text").asText());
            }
        }
        if (output.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về dữ liệu lộ trình.");
        }
        return output.toString();
    }
}
