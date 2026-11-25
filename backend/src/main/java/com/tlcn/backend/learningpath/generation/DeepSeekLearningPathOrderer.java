package com.tlcn.backend.learningpath.generation;

import com.tlcn.backend.ai.DeepSeekChatClient;
import com.tlcn.backend.learningpath.generation.LearningPathOrderer.Candidate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(prefix = "app.learning-path-generator", name = "provider", havingValue = "deepseek")
public class DeepSeekLearningPathOrderer implements LearningPathOrderer {
    private final DeepSeekChatClient deepSeek;
    private final JsonMapper mapper;

    public DeepSeekLearningPathOrderer(DeepSeekChatClient deepSeek, JsonMapper mapper) {
        this.deepSeek = deepSeek;
        this.mapper = mapper;
    }

    @Override
    public List<Integer> order(List<Candidate> candidates) {
        String output = deepSeek.completeJson(LearningPathAiSupport.orderingPrompt(candidates));
        return LearningPathAiSupport.parseOrder(output, mapper);
    }
}
