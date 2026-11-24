package com.tlcn.backend.learningpath.generation;

import com.tlcn.backend.learningpath.generation.LearningPathOrderer.Candidate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnExpression("'${APP_LEARNING_PATH_GENERATOR_PROVIDER:rules}' == 'openai' or "
        + "'${APP_LEARNING_PATH_GENERATOR_PROVIDER:rules}' == 'combined'")
public class OpenAiLearningPathOrderer implements LearningPathOrderer {
    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final JsonMapper mapper;

    @Autowired
    public OpenAiLearningPathOrderer(@Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-6.2-sol}") String model, JsonMapper mapper) {
        this(apiKey, model, LearningPathHttpClientSupport.createClient(), mapper);
    }

    OpenAiLearningPathOrderer(String apiKey, String model, RestClient client, JsonMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public List<Integer> order(List<Candidate> candidates) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình OPENAI_API_KEY.");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "instructions", "Trả về đúng JSON schema và chỉ sắp xếp các vị trí được cung cấp.",
                "input", LearningPathAiSupport.orderingPrompt(candidates),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "learning_path_order",
                        "strict", true,
                        "schema", LearningPathAiSupport.openAiOrderSchema())));
        JsonNode response = client.post().uri("https://api.openai.com/v1/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
        return LearningPathAiSupport.parseOrder(extractOutput(response), mapper);
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
                    throw new IllegalStateException("OpenAI từ chối đề xuất lộ trình.");
                }
                if ("output_text".equals(content.path("type").asText())
                        && content.path("text").isTextual()) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI không trả về thứ tự lộ trình.");
    }
}
