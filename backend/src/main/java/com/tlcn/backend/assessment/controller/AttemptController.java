package com.tlcn.backend.assessment.controller;

import com.tlcn.backend.assessment.dto.AnswerRequest;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.assessment.service.AttemptService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AttemptController {
    public record AttemptResult(Long id, Long assessmentId, String assessmentTitle, Long learningStepId, Long studentId,
            String studentName, String status, Instant startedAt, Instant submittedAt,
            double totalScore, double maxScore) {
        static AttemptResult from(AssessmentAttempt a, List<Question> questions) {
            double max = questions.stream().mapToDouble(Question::getScore).sum();
            return new AttemptResult(a.getId(), a.getAssessment() == null ? null : a.getAssessment().getId(),
                    a.getAssessment() == null ? null : a.getAssessment().getTitle(),
                    a.getLearningStep() == null ? null : a.getLearningStep().getId(),
                    a.getStudent().getId(), a.getStudent().getFullName(), a.getStatus().name(), a.getStartedAt(),
                    a.getSubmittedAt(), a.getStatus() == AttemptStatus.SUBMITTED ? a.totalScore() : 0, max);
        }
    }

    public record AnswerDetail(Long questionId, String questionContent, String kind, boolean correct,
            double score, double awardedScore, List<String> submitted, List<String> expected, String explanation) {
    }

    public record AssessmentResult(AttemptResult attempt, List<AnswerDetail> answers) {
    }

    public record AnswerDraft(Long questionId, List<Long> selectedOptionIds, List<String> blankAnswers,
            String content) {
    }

    private final AttemptService attempts;
    private final AssessmentService assessments;

    public AttemptController(AttemptService attempts, AssessmentService assessments) {
        this.attempts = attempts;
        this.assessments = assessments;
    }

    @PostMapping("/assessments/{assessmentId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AttemptResult start(@PathVariable Long assessmentId, @AuthenticationPrincipal UserSummary actor) {
        AssessmentAttempt attempt = attempts.start(assessmentId, actor);
        return AttemptResult.from(attempt, attempts.questionsForAttempt(attempt));
    }

    @PostMapping("/exercises/steps/{stepId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AttemptResult startExercise(@PathVariable Long stepId, @AuthenticationPrincipal UserSummary actor) {
        AssessmentAttempt attempt = attempts.startExercise(stepId, actor);
        return AttemptResult.from(attempt, attempts.questionsForAttempt(attempt));
    }

    @PutMapping("/attempts/{id}/answers")
    public Map<String, String> answer(@PathVariable Long id, @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        attempts.saveAnswer(id, request, actor);
        return Map.of("status", "SAVED");
    }

    @GetMapping("/attempts/student/{studentId}")
    @Transactional(readOnly = true)
    public List<AttemptResult> byStudent(@PathVariable Long studentId,
            @RequestParam(required = false) Long classroomId,
            @AuthenticationPrincipal UserSummary actor) {
        return assessments.attemptsForStudent(studentId, classroomId, actor).stream()
                .map(attempt -> AttemptResult.from(attempt, attempts.questionsForAttempt(attempt))).toList();
    }

    @GetMapping("/attempts/{id}")
    @Transactional(readOnly = true)
    public AttemptResult get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        AssessmentAttempt attempt = attempts.get(id, actor);
        return AttemptResult.from(attempt, attempts.questionsForAttempt(attempt));
    }

    @GetMapping("/attempts/{id}/draft")
    @Transactional(readOnly = true)
    public List<AnswerDraft> draft(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        if (actor == null || actor.role() != com.tlcn.backend.user.entity.Role.STUDENT)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ học sinh mới được xem câu trả lời đang lưu.");
        AssessmentAttempt attempt = attempts.get(id, actor);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài làm này đã nộp.");
        return attempt.getAnswers().stream().map(answer -> {
            if (answer instanceof MultipleChoiceAnswer selected)
                return new AnswerDraft(answer.getQuestion().getId(),
                        selected.getSelectedOptions().stream().map(Option::getId).toList(), List.of(), null);
            if (answer instanceof FillBlankAnswer filled)
                return new AnswerDraft(answer.getQuestion().getId(), List.of(),
                        filled.getAnswers().stream().sorted(Comparator.comparing(item -> item.getBlank().getPosition()))
                                .map(StudentBlankAnswer::getContent).toList(),
                        null);
            ShortAnswerAnswer shortAnswer = (ShortAnswerAnswer) answer;
            return new AnswerDraft(answer.getQuestion().getId(), List.of(), List.of(), shortAnswer.getContent());
        }).toList();
    }

    @PostMapping("/attempts/{id}/submit")
    @Transactional
    public AttemptResult submit(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        AssessmentAttempt attempt = attempts.submit(id, actor);
        return AttemptResult.from(attempt, attempts.questionsForAttempt(attempt));
    }

    @GetMapping("/attempts/{id}/result")
    @Transactional(readOnly = true)
    public AssessmentResult result(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        AssessmentAttempt attempt = attempts.get(id, actor);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kết quả sẽ hiển thị sau khi nộp bài.");
        Map<Long, StudentAnswer> saved = new HashMap<>();
        attempt.getAnswers().forEach(answer -> saved.put(answer.getQuestion().getId(), answer));
        List<Question> questions = attempts.questionsForAttempt(attempt);
        List<AnswerDetail> details = questions.stream().map(question -> {
            StudentAnswer answer = saved.get(question.getId());
            List<String> submitted = new ArrayList<>();
            List<String> expected = new ArrayList<>();
            String kind;
            if (question instanceof MultipleChoiceQuestion mc) {
                kind = "MULTIPLE_CHOICE";
                expected.addAll(mc.getOptions().stream().filter(Option::isCorrect)
                        .sorted(Comparator.comparing(Option::getDisplayOrder)).map(Option::getName).toList());
                if (answer instanceof MultipleChoiceAnswer selected)
                    submitted.addAll(selected.getSelectedOptions().stream()
                            .sorted(Comparator.comparing(Option::getDisplayOrder)).map(Option::getName).toList());
            } else if (question instanceof FillBlankQuestion fill) {
                kind = "FILL_BLANK";
                expected.addAll(fill.getBlanks().stream().sorted(Comparator.comparing(Blank::getPosition))
                        .map(Blank::getCorrectAnswer).toList());
                if (answer instanceof FillBlankAnswer filled)
                    submitted.addAll(filled.getAnswers().stream()
                            .sorted(Comparator.comparing(item -> item.getBlank().getPosition()))
                            .map(StudentBlankAnswer::getContent).toList());
            } else if (question instanceof ShortAnswerQuestion shortQuestion) {
                kind = "SHORT_ANSWER";
                expected.add(shortQuestion.getCorrectAnswer());
                if (answer instanceof ShortAnswerAnswer shortAnswer)
                    submitted.add(shortAnswer.getContent());
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không hỗ trợ dạng câu hỏi của bài làm này.");
            }
            return new AnswerDetail(question.getId(), question.getContent(), kind, answer != null && answer.isCorrect(),
                    question.getScore(), answer == null ? 0 : answer.score(), submitted, expected,
                    question.getExplanation());
        }).toList();
        return new AssessmentResult(AttemptResult.from(attempt, questions), details);
    }
}
