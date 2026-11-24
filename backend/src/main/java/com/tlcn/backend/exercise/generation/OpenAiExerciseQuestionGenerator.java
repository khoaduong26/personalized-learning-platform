package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.learningpath.entity.LearningStep;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.exercise-generator", name = "provider", havingValue = "openai")
public class OpenAiExerciseQuestionGenerator implements ExerciseQuestionGenerator {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final JsonMapper mapper;

    @Autowired
    public OpenAiExerciseQuestionGenerator(@Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-6.2-sol}") String model, JsonMapper mapper) {
        this(apiKey, model, ExerciseGeneratorHttpClientSupport.createClient(), mapper);
    }

    OpenAiExerciseQuestionGenerator(String apiKey, String model, RestClient client, JsonMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public GeneratedExercise generate(LearningStep step) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình OPENAI_API_KEY.");
        }
        Map<String, Object> format = Map.of(
                "type", "json_schema",
                "name", "grade_2_math_exercise",
                "strict", true,
                "schema", ExerciseGeneratorSupport.openAiSchema());
        Map<String, Object> body = Map.of(
                "model", model,
                "instructions", "Bạn là trợ lý soạn câu hỏi Toán lớp 2. Trả về đúng cấu trúc được yêu cầu.",
                "input", ExerciseGeneratorSupport.prompt(step),
                "text", Map.of("format", format));

        JsonNode response = client.post().uri("https://api.openai.com/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        String output = extractOutput(response);
        return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "openai", mapper);
    }

    private String extractOutput(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI trả về phản hồi rỗng.");
        }
        if (response.path("output_text").isTextual()) {
            return response.path("output_text").asText();
        }
        for (JsonNode item : response.path("output")) {
            for (JsonNode content : item.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new IllegalStateException("OpenAI từ chối tạo câu hỏi.");
                }
                if ("output_text".equals(content.path("type").asText())
                        && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI không trả về nội dung câu hỏi hoàn chỉnh.");
    }
}
