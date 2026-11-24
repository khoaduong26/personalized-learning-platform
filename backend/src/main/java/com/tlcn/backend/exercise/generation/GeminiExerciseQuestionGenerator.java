package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.learningpath.entity.LearningStep;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnExpression("'${app.exercise-generator.provider:demo}' == 'gemini' or "
        + "'${app.exercise-generator.provider:demo}' == 'combined'")
public class GeminiExerciseQuestionGenerator implements ExerciseQuestionGenerator, ExerciseQuestionReviewer {
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
        String output = generateJson(ExerciseGeneratorSupport.prompt(step), ExerciseGeneratorSupport.geminiSchema());
        return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "gemini", mapper);
    }

    @Override
    public ReviewResult review(LearningStep step, QuestionRequest question) {
        String output = generateJson(ExerciseGeneratorSupport.reviewPrompt(step, question),
                ExerciseGeneratorSupport.geminiReviewSchema());
        try {
            return mapper.readValue(output, ReviewResult.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Gemini trả về kết quả rà soát không hợp lệ.", exception);
        }
    }

    private String generateJson(String prompt, Map<String, Object> schema) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY.");
        }
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json",
                "responseJsonSchema", schema,
                "temperature", 0.2,
                "maxOutputTokens", 1000);
        Map<String, Object> body = Map.of(
                "contents", new Object[]{Map.of("parts", new Object[]{Map.of("text", prompt)})},
                "generationConfig", generationConfig);

        JsonNode response = client.post().uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return extractOutput(response);
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
