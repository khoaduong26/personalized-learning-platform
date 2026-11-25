package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.ai.DeepSeekChatClient;
import com.tlcn.backend.learningpath.entity.LearningStep;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@ConditionalOnProperty(prefix = "app.exercise-generator", name = "provider", havingValue = "deepseek")
public class DeepSeekExerciseQuestionGenerator implements ExerciseQuestionGenerator {
    private final DeepSeekChatClient deepSeek;
    private final JsonMapper mapper;

    public DeepSeekExerciseQuestionGenerator(DeepSeekChatClient deepSeek, JsonMapper mapper) {
        this.deepSeek = deepSeek;
        this.mapper = mapper;
    }

    @Override
    public GeneratedExercise generate(LearningStep step) {
        String output = deepSeek.completeJson(ExerciseGeneratorSupport.prompt(step));
        return ExerciseGeneratorSupport.parse(output, step.getLesson().getId(), "deepseek", mapper);
    }
}
