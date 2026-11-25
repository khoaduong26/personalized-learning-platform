package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.ai.DeepSeekChatClient;
import com.tlcn.backend.ai.GeminiFallbackSupport;
import com.tlcn.backend.learningpath.entity.LearningStep;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.exercise-generator", name = "provider", havingValue = "gemini")
public class GeminiExerciseQuestionGenerator implements ExerciseQuestionGenerator {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final JsonMapper mapper;
    private final DeepSeekChatClient deepSeekClient;
    private final String fallbackProvider;

    @Autowired
    public GeminiExerciseQuestionGenerator(@Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-3.8-flash}") String model,
            @Value("${app.ai.fallback-provider:deepseek}") String fallbackProvider,
            JsonMapper mapper, DeepSeekChatClient deepSeekClient) {
        this(apiKey, model, ExerciseGeneratorHttpClientSupport.createClient(), mapper,
                fallbackProvider, deepSeekClient);
    }

    GeminiExerciseQuestionGenerator(String apiKey, String model, RestClient client, JsonMapper mapper) {
        this(apiKey, model, client, mapper, "deepseek", null);
    }

    GeminiExerciseQuestionGenerator(String apiKey, String model, RestClient client, JsonMapper mapper,
            String fallbackProvider, DeepSeekChatClient deepSeekClient) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = client;
        this.mapper = mapper;
        this.fallbackProvider = fallbackProvider;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public GeneratedExercise generate(LearningStep step) {
        String prompt = ExerciseGeneratorSupport.prompt(step);
        try {
            String output = generateJson(prompt, ExerciseGeneratorSupport.geminiSchema());
            return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "gemini", mapper);
        } catch (RuntimeException exception) {
            if (!canFallback(exception)) {
                throw exception;
            }
            String output = deepSeekClient.completeJson(prompt);
            return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "deepseek", mapper);
        }
    }

    private boolean canFallback(RuntimeException exception) {
        return "deepseek".equalsIgnoreCase(fallbackProvider)
                && deepSeekClient != null
                && deepSeekClient.isConfigured()
                && GeminiFallbackSupport.shouldUseDeepSeek(exception);
    }

    private String generateJson(String prompt, Map<String, Object> schema) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "input", prompt,
                "response_format", Map.of(
                        "type", "text",
                        "mime_type", "application/json",
                        "schema", schema),
                "generation_config", Map.of(
                        "temperature", 0.2,
                        "max_output_tokens", 1000));

        JsonNode response = client.post().uri("https://generativelanguage.googleapis.com/v1beta/interactions")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return extractOutput(response);
    }

    private String extractOutput(JsonNode response) {
        if (response == null || !"completed".equals(response.path("status").asText())) {
            throw new IllegalStateException("Gemini không trả về câu hỏi hoàn chỉnh.");
        }
        StringBuilder output = new StringBuilder();
        for (JsonNode step : response.path("steps")) {
            if ("model_output".equals(step.path("type").asText())) {
                for (JsonNode part : step.path("content")) {
                    if (part.path("text").isTextual()) {
                        output.append(part.path("text").asText());
                    }
                }
            }
        }
        if (output.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về nội dung câu hỏi.");
        }
        return output.toString();
    }
}
