package com.tlcn.backend.exercise.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.learningpath.entity.LearningStep;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderExerciseQuestionGeneratorTests {
    private static final Long LESSON_ID = 77L;
    private static final String MODEL_JSON = "{\"content\":\"7 + 5 = ?\",\"difficulty\":\"EASY\","
            + "\"explanation\":\"7 cộng 5 bằng 12.\",\"options\":["
            + "{\"name\":\"11\",\"correct\":false,\"displayOrder\":0},"
            + "{\"name\":\"12\",\"correct\":true,\"displayOrder\":1},"
            + "{\"name\":\"13\",\"correct\":false,\"displayOrder\":2}]}";

    @Test
    void openAiAdapterUsesResponsesStructuredOutputAndBindsLessonOnServer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai-key"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.text.format.schema.additionalProperties").value(false))
                .andRespond(withSuccess(openAiResponse(), MediaType.APPLICATION_JSON));

        var adapter = new OpenAiExerciseQuestionGenerator("test-openai-key", "test-model", builder.build(),
                JsonMapper.builder().build());
        var generated = adapter.generate(step());

        assertEquals("openai", generated.provider());
        assertFalse(generated.demo());
        assertEquals(LESSON_ID, generated.question().lessonId());
        assertEquals("7 + 5 = ?", generated.question().content());
        assertEquals(3, generated.question().options().size());
        assertTrue(generated.notice().contains("giáo viên"));
        server.verify();
    }

    @Test
    void geminiAdapterUsesNativeStructuredOutputAndBindsLessonOnServer() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/test-gemini:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-gemini-key"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseJsonSchema.type").value("OBJECT"))
                .andRespond(withSuccess(geminiResponse(), MediaType.APPLICATION_JSON));

        var adapter = new GeminiExerciseQuestionGenerator("test-gemini-key", "test-gemini", builder.build(),
                JsonMapper.builder().build());
        var generated = adapter.generate(step());

        assertEquals("gemini", generated.provider());
        assertFalse(generated.demo());
        assertEquals(LESSON_ID, generated.question().lessonId());
        assertEquals("7 + 5 = ?", generated.question().content());
        assertEquals(1, generated.question().options().stream().filter(option -> option.correct()).count());
        server.verify();
    }

    private LearningStep step() {
        Lesson lesson = mock(Lesson.class);
        when(lesson.getId()).thenReturn(LESSON_ID);
        when(lesson.getName()).thenReturn("Cộng có nhớ");
        when(lesson.getDescription()).thenReturn("Cộng các số trong phạm vi 100.");
        LearningStep step = mock(LearningStep.class);
        when(step.getLesson()).thenReturn(lesson);
        return step;
    }

    private String openAiResponse() {
        return "{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":"
                + quote(MODEL_JSON) + "}]}]}";
    }

    private String geminiResponse() {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(MODEL_JSON)
                + "}]},\"finishReason\":\"STOP\"}]}";
    }

    private String quote(String value) {
        try {
            return JsonMapper.builder().build().writeValueAsString(value);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
