package com.tlcn.backend.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class DeepSeekChatClient {
    private final String apiKey;
    private final String model;
    private final RestClient client;

    public DeepSeekChatClient(@Value("${DEEPSEEK_API_KEY:}") String apiKey,
            @Value("${DEEPSEEK_MODEL:deepseek-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.client = RestClient.builder()
                .baseUrl("https://api.deepseek.com")
                .requestFactory(factory)
                .build();
    }

    public String completeJson(String prompt) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình DEEPSEEK_API_KEY.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Return one valid JSON object only, without Markdown."),
                        Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_object"),
                "thinking", Map.of("type", "disabled"),
                "temperature", 0.2,
                "max_tokens", 1000,
                "stream", false);

        JsonNode response = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        JsonNode choice = response == null ? null : response.path("choices").path(0);
        if (choice == null || choice.isMissingNode() || !"stop".equals(choice.path("finish_reason").asText())) {
            throw new IllegalStateException("DeepSeek không trả về kết quả JSON hoàn chỉnh.");
        }
        JsonNode content = choice.path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IllegalStateException("DeepSeek không trả về nội dung JSON.");
        }
        return content.asText();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }
}
