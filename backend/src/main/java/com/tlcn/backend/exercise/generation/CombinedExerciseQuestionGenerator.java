package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.learningpath.entity.LearningStep;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(prefix = "app.exercise-generator", name = "provider", havingValue = "combined")
public class CombinedExerciseQuestionGenerator implements ExerciseQuestionGenerator {
    private static final String NOTICE = "OpenAI đã tạo câu hỏi và Gemini đã rà soát sơ bộ. "
            + "Đây không thay thế việc giáo viên kiểm tra và duyệt bài.";

    private final OpenAiExerciseQuestionGenerator generator;
    private final ExerciseQuestionReviewer reviewer;

    public CombinedExerciseQuestionGenerator(OpenAiExerciseQuestionGenerator generator,
            ExerciseQuestionReviewer reviewer) {
        this.generator = generator;
        this.reviewer = reviewer;
    }

    @Override
    public GeneratedExercise generate(LearningStep step) {
        GeneratedExercise draft = generator.generate(step);
        if (draft == null || draft.question() == null) {
            throw new IllegalStateException("OpenAI không tạo được bản nháp bài tập.");
        }

        ExerciseQuestionReviewer.ReviewResult review = reviewer.review(step, draft.question());
        if (review == null || !review.accepted()) {
            throw new IllegalStateException("Gemini chưa chấp nhận bản nháp bài tập.");
        }

        return new GeneratedExercise(draft.question(), "openai+gemini", false, NOTICE);
    }
}
