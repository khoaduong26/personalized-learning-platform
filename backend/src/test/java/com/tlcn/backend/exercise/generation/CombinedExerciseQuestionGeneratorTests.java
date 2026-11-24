package com.tlcn.backend.exercise.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.entity.Difficulty;
import com.tlcn.backend.learningpath.entity.LearningStep;
import java.util.List;
import org.junit.jupiter.api.Test;

class CombinedExerciseQuestionGeneratorTests {
    @Test
    void returnsOpenAiDraftOnlyAfterGeminiAcceptsIt() {
        LearningStep step = step();
        OpenAiExerciseQuestionGenerator openAi = mock(OpenAiExerciseQuestionGenerator.class);
        ExerciseQuestionReviewer gemini = mock(ExerciseQuestionReviewer.class);
        QuestionRequest question = question();
        when(openAi.generate(step)).thenReturn(new ExerciseQuestionGenerator.GeneratedExercise(
                question, "openai", false, "OpenAI draft"));
        when(gemini.review(step, question)).thenReturn(new ExerciseQuestionReviewer.ReviewResult(true, "Đạt."));
        CombinedExerciseQuestionGenerator combined = new CombinedExerciseQuestionGenerator(openAi, gemini);

        var result = combined.generate(step);

        assertEquals(question, result.question());
        assertEquals("openai+gemini", result.provider());
        assertFalse(result.demo());
        assertTrue(result.notice().contains("giáo viên"));
        verify(gemini).review(step, question);
    }

    @Test
    void doesNotReturnDraftWhenGeminiRejectsIt() {
        LearningStep step = step();
        OpenAiExerciseQuestionGenerator openAi = mock(OpenAiExerciseQuestionGenerator.class);
        ExerciseQuestionReviewer gemini = mock(ExerciseQuestionReviewer.class);
        QuestionRequest question = question();
        when(openAi.generate(step)).thenReturn(new ExerciseQuestionGenerator.GeneratedExercise(
                question, "openai", false, "OpenAI draft"));
        when(gemini.review(step, question)).thenReturn(new ExerciseQuestionReviewer.ReviewResult(false, "Sai đáp án."));
        CombinedExerciseQuestionGenerator combined = new CombinedExerciseQuestionGenerator(openAi, gemini);

        assertThrows(IllegalStateException.class, () -> combined.generate(step));
        verify(gemini).review(step, question);
    }

    private LearningStep step() {
        Lesson lesson = mock(Lesson.class);
        when(lesson.getId()).thenReturn(77L);
        when(lesson.getName()).thenReturn("Cộng có nhớ");
        LearningStep step = mock(LearningStep.class);
        when(step.getLesson()).thenReturn(lesson);
        return step;
    }

    private QuestionRequest question() {
        return new QuestionRequest("7 + 5 = ?", Difficulty.EASY, 1.0, 77L,
                "MULTIPLE_CHOICE", false, null, null,
                List.of(new QuestionRequest.OptionInput("12", true, 0),
                        new QuestionRequest.OptionInput("13", false, 1)), null, "7 cộng 5 bằng 12.");
    }
}
