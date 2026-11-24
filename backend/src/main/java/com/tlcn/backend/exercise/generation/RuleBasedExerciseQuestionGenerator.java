package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.entity.Difficulty;
import com.tlcn.backend.learningpath.entity.LearningStep;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.exercise-generator", name = "provider", havingValue = "demo", matchIfMissing = true)
public class RuleBasedExerciseQuestionGenerator implements ExerciseQuestionGenerator {
    private static final String NOTICE = "Bài tập minh họa do bộ quy tắc demo tạo, chưa kết nối AI bên ngoài. "
            + "Giáo viên cần rà soát trước khi duyệt.";

    @Override
    public GeneratedExercise generate(LearningStep step) {
        String lessonName = step.getLesson().getName();
        String normalizedName = normalize(lessonName);
        Example example = exampleFor(normalizedName);
        String content = "[DEMO] " + lessonName + ": " + example.prompt();
        QuestionRequest request = new QuestionRequest(content, Difficulty.EASY, 1.0, step.getLesson().getId(),
                "MULTIPLE_CHOICE", false, null, null,
                List.of(
                        new QuestionRequest.OptionInput(example.wrongAnswers().get(0), false, 0),
                        new QuestionRequest.OptionInput(example.answer(), true, 1),
                        new QuestionRequest.OptionInput(example.wrongAnswers().get(1), false, 2)),
                null, example.explanation());
        return new GeneratedExercise(request, "demo-rule-based", true, NOTICE);
    }

    private Example exampleFor(String lessonName) {
        if (lessonName.contains("tru") || lessonName.contains("subtract")) {
            return new Example("8 − 3 = ?", "5", List.of("4", "6"), "8 trừ 3 bằng 5.");
        }
        if (lessonName.contains("nhan") || lessonName.contains("multiply")) {
            return new Example("2 × 3 = ?", "6", List.of("5", "8"), "2 nhân 3 bằng 6.");
        }
        if (lessonName.contains("chia") || lessonName.contains("divid")) {
            return new Example("6 ÷ 2 = ?", "3", List.of("2", "4"), "6 chia 2 bằng 3.");
        }
        return new Example("2 + 3 = ?", "5", List.of("4", "6"), "2 cộng 3 bằng 5.");
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private record Example(String prompt, String answer, List<String> wrongAnswers, String explanation) {
    }
}
