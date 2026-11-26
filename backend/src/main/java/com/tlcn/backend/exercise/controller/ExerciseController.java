package com.tlcn.backend.exercise.controller;

import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.dto.QuestionView;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.exercise.service.ExerciseService;
import com.tlcn.backend.exercise.service.ExerciseGenerationResult;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    public record ReviewRequest(ApprovalStatus status) {
    }

    public record ReviewOption(Long id, String name, boolean correct, Integer displayOrder) {
    }

    public record ReviewView(Long questionId, String content, String difficulty, Double score, Long lessonId,
            String kind, String status, String explanation, boolean allowMultiple, List<ReviewOption> options,
            List<String> expectedAnswers) {
        static ReviewView from(Question question) {
            question = (Question) Hibernate.unproxy(question);
            if (question instanceof MultipleChoiceQuestion multiple) {
                return new ReviewView(question.getId(), question.getContent(), question.getDifficulty().name(),
                        question.getScore(), question.getLesson().getId(), "MULTIPLE_CHOICE",
                        question.getApprovalStatus().name(), question.getExplanation(), multiple.isAllowMultiple(),
                        multiple.getOptions().stream().map(option -> new ReviewOption(option.getId(), option.getName(),
                                option.isCorrect(), option.getDisplayOrder())).toList(), List.of());
            }
            if (question instanceof FillBlankQuestion fill) {
                return new ReviewView(question.getId(), question.getContent(), question.getDifficulty().name(),
                        question.getScore(), question.getLesson().getId(), "FILL_BLANK",
                        question.getApprovalStatus().name(), question.getExplanation(), false, List.of(),
                        fill.getBlanks().stream().map(Blank::getCorrectAnswer).toList());
            }
            ShortAnswerQuestion shortAnswer = (ShortAnswerQuestion) question;
            return new ReviewView(question.getId(), question.getContent(), question.getDifficulty().name(),
                    question.getScore(), question.getLesson().getId(), "SHORT_ANSWER",
                    question.getApprovalStatus().name(), question.getExplanation(), false, List.of(),
                    List.of(shortAnswer.getCorrectAnswer()));
        }
    }

    public record GenerationView(ReviewView question, String provider, boolean demo, String notice) {
        static GenerationView from(ExerciseGenerationResult result) {
            return new GenerationView(ReviewView.from(result.question()), result.provider(), result.demo(), result.notice());
        }
    }

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @GetMapping("/steps/{stepId}")
    @Transactional(readOnly = true)
    public List<QuestionView> forStep(@PathVariable Long stepId, @AuthenticationPrincipal UserSummary actor) {
        return service.forStep(stepId, actor).stream().map(e -> QuestionView.from(e.getQuestion())).toList();
    }

    @PostMapping("/steps/{stepId}/generated")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReviewView createGenerated(@PathVariable Long stepId, @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return ReviewView.from(service.createGenerated(stepId, request, actor));
    }

    @PostMapping("/steps/{stepId}/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GenerationView generate(@PathVariable Long stepId, @AuthenticationPrincipal UserSummary actor) {
        return GenerationView.from(service.generateForStep(stepId, actor));
    }

    @GetMapping("/pending-review")
    @Transactional(readOnly = true)
    public List<ReviewView> pending(@AuthenticationPrincipal UserSummary actor) {
        return service.pendingReview(actor).stream().map(ReviewView::from).toList();
    }

    @PatchMapping("/questions/{questionId}")
    @Transactional
    public ReviewView update(@PathVariable Long questionId, @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return ReviewView.from(service.updateGenerated(questionId, request, actor));
    }

    @PatchMapping("/questions/{questionId}/review")
    @Transactional
    public ReviewView review(@PathVariable Long questionId, @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return ReviewView.from(service.review(questionId, request.status(), actor));
    }
}
