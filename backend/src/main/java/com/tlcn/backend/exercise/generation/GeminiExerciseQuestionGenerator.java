package com.tlcn.backend.exercise.generation;

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

    @Autowired
    public GeminiExerciseQuestionGenerator(@Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${GEMINI_MODEL:gemini-3.8-flash}") String model, JsonMapper mapper) {
        this(apiKey, model, ExerciseGeneratorHttpClientSupport.createClient(), mapper);
    }

    GeminiExerciseQuestionGenerator(String apiKey, String model, RestClient client, JsonMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public GeneratedExercise generate(LearningStep step) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY.");
        }
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseJsonSchema", ExerciseGeneratorSupport.geminiSchema(),
                "temperature", 0.2,
                "maxOutputTokens", 1000);
        Map<String, Object> body = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", ExerciseGeneratorSupport.prompt(step))})},
                "generationConfig", generationConfig);

        JsonNode response = client.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        String output = extractOutput(response);
        return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "gemini", mapper);
    }

    private String extractOutput(JsonNode response) {
        JsonNode candidate = response == null ? null : response.path("candidates").path(0);
        if (candidate == null || candidate.isMissingNode()
                || !"STOP".equals(candidate.path("finishReason").asText())) {
            throw new IllegalStateException("Gemini không trả về câu hỏi hoàn chỉnh.");
        }
        StringBuilder output = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("text").isTextual()) {
                output.append(part.path("text").asText());
            }
        }
        if (output.isEmpty()) {
            throw new IllegalStateException("Gemini không trả về nội dung câu hỏi.");
        }
        return output.toString();
    }
}
