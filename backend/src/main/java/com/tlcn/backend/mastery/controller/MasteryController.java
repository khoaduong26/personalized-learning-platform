package com.tlcn.backend.mastery.controller;

import com.tlcn.backend.mastery.entity.*;
import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.user.dto.UserSummary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/students/{studentId}/mastery")
public class MasteryController {
    public record MasteryView(Long id, Long lessonId, String lessonName, Double score, String level, Double confidence,
            Instant updatedAt) {
        static MasteryView from(KnowledgeMastery m) {
            return new MasteryView(m.getId(), m.getLesson().getId(), m.getLesson().getName(), m.getMasteryScore(), m.getMasteryLevel().name(),
                    m.getConfidenceScore(), m.getLastUpdated());
        }
    }

    public record ErrorView(Long id, Long lessonId, String lessonName, String errorType, String description,
            Long attemptId, String assessmentTitle, Instant submittedAt, Long questionId, String questionContent,
            String questionKind, List<String> studentAnswers, List<String> correctAnswers, double awardedScore,
            double score, String explanation, Instant createdAt) {
        static ErrorView from(StudentError e) {
            StudentAnswer answer = e.getAnswer();
            if (answer == null) {
                return new ErrorView(e.getId(), e.getLesson().getId(), e.getLesson().getName(), e.getErrorType(),
                        e.getDescription(), null, null, null, null, null, null, List.of(), List.of(), 0, 0, null,
                        e.getCreatedAt());
            }
            Question question = answer.getQuestion();
            List<String> studentAnswers = new java.util.ArrayList<>();
            List<String> correctAnswers = new java.util.ArrayList<>();
            String kind;
            if (question instanceof MultipleChoiceQuestion multipleChoice) {
                kind = "MULTIPLE_CHOICE";
                multipleChoice.getOptions().stream().filter(Option::isCorrect)
                        .sorted(java.util.Comparator.comparing(Option::getDisplayOrder))
                        .map(Option::getName).forEach(correctAnswers::add);
                if (answer instanceof MultipleChoiceAnswer selected)
                    selected.getSelectedOptions().stream()
                            .sorted(java.util.Comparator.comparing(Option::getDisplayOrder))
                            .map(Option::getName).forEach(studentAnswers::add);
            } else if (question instanceof FillBlankQuestion fillBlank) {
                kind = "FILL_BLANK";
                fillBlank.getBlanks().stream().sorted(java.util.Comparator.comparing(Blank::getPosition))
                        .map(Blank::getCorrectAnswer).forEach(correctAnswers::add);
                if (answer instanceof FillBlankAnswer filled)
                    filled.getAnswers().stream()
                            .sorted(java.util.Comparator.comparing(item -> item.getBlank().getPosition()))
                            .map(StudentBlankAnswer::getContent).forEach(studentAnswers::add);
            } else if (question instanceof ShortAnswerQuestion shortQuestion) {
                kind = "SHORT_ANSWER";
                correctAnswers.add(shortQuestion.getCorrectAnswer());
                if (answer instanceof ShortAnswerAnswer shortAnswer)
                    studentAnswers.add(shortAnswer.getContent());
            } else {
                kind = "UNKNOWN";
            }

            AssessmentAttempt attempt = answer.getAttempt();
            String title = null;
            if (attempt != null && attempt.getAssessment() != null)
                title = attempt.getAssessment().getTitle();
            else if (attempt != null && attempt.getLearningStep() != null)
                title = "Bài tập · " + attempt.getLearningStep().getLesson().getName();
            return new ErrorView(e.getId(), e.getLesson().getId(), e.getLesson().getName(), e.getErrorType(),
                    e.getDescription(), attempt == null ? null : attempt.getId(), title,
                    attempt == null ? answer.getSubmittedAt() : attempt.getSubmittedAt(), question.getId(),
                    question.getContent(), kind, studentAnswers, correctAnswers, answer.score(), question.getScore(),
                    question.getExplanation(), e.getCreatedAt());
        }
    }

    public record GapView(Long id, Long lessonId, String lessonName, String priority, String rootCause, String status,
            Instant identifiedAt) {
        static GapView from(KnowledgeGap g) {
            return new GapView(g.getId(), g.getLesson().getId(), g.getLesson().getName(), g.getPriority().name(), g.getRootCause(),
                    g.getStatus().name(), g.getIdentifiedAt());
        }
    }

    private final MasteryService service;

    public MasteryController(MasteryService service) {
        this.service = service;
    }

    @GetMapping
    public List<MasteryView> profile(@PathVariable Long studentId, @AuthenticationPrincipal UserSummary actor) {
        service.requireCanView(studentId, actor);
        return service.profile(studentId).stream().map(MasteryView::from).toList();
    }

    @GetMapping("/errors")
    public List<ErrorView> errors(@PathVariable Long studentId, @AuthenticationPrincipal UserSummary actor) {
        service.requireCanView(studentId, actor);
        return service.errors(studentId).stream().map(ErrorView::from).toList();
    }

    @GetMapping("/gaps")
    public List<GapView> gaps(@PathVariable Long studentId, @AuthenticationPrincipal UserSummary actor) {
        service.requireCanView(studentId, actor);
        return service.gaps(studentId).stream().map(GapView::from).toList();
    }

}
