package com.tlcn.backend.assessment.service.impl;

import com.tlcn.backend.assessment.dto.AnswerRequest;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.*;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.assessment.service.AttemptService;
import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.repository.StudentRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttemptServiceImpl implements AttemptService {
    private final AssessmentAttemptRepository attempts;
    private final StudentAnswerRepository answers;
    private final MultipleChoiceAnswerRepository multipleAnswers;
    private final FillBlankAnswerRepository fillAnswers;
    private final ShortAnswerAnswerRepository shortAnswers;
    private final StudentRepository students;
    private final AssessmentService assessments;
    private final MasteryService mastery;

    public AttemptServiceImpl(AssessmentAttemptRepository attempts, StudentAnswerRepository answers,
            MultipleChoiceAnswerRepository multipleAnswers, FillBlankAnswerRepository fillAnswers,
            ShortAnswerAnswerRepository shortAnswers, StudentRepository students, AssessmentService assessments,
            MasteryService mastery) {
        this.attempts = attempts;
        this.answers = answers;
        this.multipleAnswers = multipleAnswers;
        this.fillAnswers = fillAnswers;
        this.shortAnswers = shortAnswers;
        this.students = students;
        this.assessments = assessments;
        this.mastery = mastery;
    }

    @Transactional(readOnly = true)
    public AssessmentAttempt get(Long id, UserSummary actor) {
        return accessibleAttempt(id, actor);
    }

    @Transactional
    public AssessmentAttempt start(Long assessmentId, UserSummary actor) {
        requireStudent(actor);
        Assessment assessment = assessments.get(assessmentId, actor);
        if (!assessment.isPublished())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bài kiểm tra chưa được giao.");
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessment(assessment);
        attempt.setStudent(students.findById(actor.id()).orElseThrow(() -> forbidden()));
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return attempts.save(attempt);
    }

    @Transactional
    public StudentAnswer saveAnswer(Long attemptId, AnswerRequest request, UserSummary actor) {
        requireStudent(actor);
        AssessmentAttempt attempt = accessibleAttempt(attemptId, actor);
        requireInProgress(attempt, true);
        Question question = attempt.getAssessment().getQuestions().stream()
                .filter(value -> value.getId().equals(request.questionId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Câu hỏi không thuộc bài kiểm tra này."));
        answers.findByAttemptIdAndQuestionId(attemptId, question.getId()).ifPresent(existing -> {
            answers.delete(existing);
            answers.flush();
        });
        if (question instanceof MultipleChoiceQuestion mc) {
            Set<Long> ids = new HashSet<>(
                    request.selectedOptionIds() == null ? List.of() : request.selectedOptionIds());
            List<Option> selected = mc.getOptions().stream().filter(option -> ids.contains(option.getId())).toList();
            if (selected.size() != ids.size())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lựa chọn không hợp lệ.");
            MultipleChoiceAnswer answer = new MultipleChoiceAnswer();
            answer.setQuestion(question);
            answer.setAttempt(attempt);
            answer.setSelectedOptions(new HashSet<>(selected));
            return multipleAnswers.save(answer);
        }
        if (question instanceof FillBlankQuestion fill) {
            List<String> values = request.blankAnswers() == null ? List.of() : request.blankAnswers();
            if (values.size() != fill.getBlanks().size())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số câu trả lời chưa khớp với số ô trống.");
            FillBlankAnswer answer = new FillBlankAnswer();
            answer.setQuestion(question);
            answer.setAttempt(attempt);
            List<StudentBlankAnswer> items = new ArrayList<>();
            for (int i = 0; i < fill.getBlanks().size(); i++) {
                StudentBlankAnswer item = new StudentBlankAnswer();
                item.setBlank(fill.getBlanks().get(i));
                item.setContent(values.get(i) == null ? "" : values.get(i).strip());
                items.add(item);
            }
            answer.setAnswers(items);
            return fillAnswers.save(answer);
        }
        if (question instanceof ShortAnswerQuestion) {
            ShortAnswerAnswer answer = new ShortAnswerAnswer();
            answer.setQuestion(question);
            answer.setAttempt(attempt);
            answer.setContent(request.content() == null ? "" : request.content().strip());
            return shortAnswers.save(answer);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dạng câu hỏi không được hỗ trợ.");
    }

    @Transactional
    public AssessmentAttempt submit(Long id, UserSummary actor) {
        requireStudent(actor);
        AssessmentAttempt attempt = accessibleAttempt(id, actor);
        requireInProgress(attempt, false);
        for (StudentAnswer answer : answers.findByAttemptId(id))
            grade(answer);
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        AssessmentAttempt submitted = attempts.save(attempt);
        mastery.analyze(submitted);
        return submitted;
    }

    private void grade(StudentAnswer answer) {
        Question question = answer.getQuestion();
        double max = question.getScore();
        if (question instanceof MultipleChoiceQuestion mc && answer instanceof MultipleChoiceAnswer selected) {
            Set<Long> expected = mc.getOptions().stream().filter(Option::isCorrect).map(Option::getId)
                    .collect(java.util.stream.Collectors.toSet());
            Set<Long> actual = selected.getSelectedOptions().stream().map(Option::getId)
                    .collect(java.util.stream.Collectors.toSet());
            answer.setCorrect(expected.equals(actual));
            answer.setAwardedScore(expected.equals(actual) ? max : 0.0);
        } else if (question instanceof FillBlankQuestion fill && answer instanceof FillBlankAnswer filled) {
            if (fill.getBlanks().isEmpty()) {
                answer.setCorrect(false);
                answer.setAwardedScore(0.0);
                answers.save(answer);
                return;
            }
            Map<Long, String> submitted = new HashMap<>();
            filled.getAnswers().forEach(item -> submitted.put(item.getBlank().getId(), item.getContent()));
            int correct = 0;
            for (Blank blank : fill.getBlanks())
                if (matches(submitted.get(blank.getId()), blank.getCorrectAnswer(), fill.isCaseSensitive()))
                    correct++;
            answer.setCorrect(correct == fill.getBlanks().size());
            answer.setAwardedScore(max * correct / fill.getBlanks().size());
        } else if (question instanceof ShortAnswerQuestion shortQ && answer instanceof ShortAnswerAnswer shortA) {
            boolean correct = matches(shortA.getContent(), shortQ.getCorrectAnswer(), shortQ.isCaseSensitive());
            answer.setCorrect(correct);
            answer.setAwardedScore(correct ? max : 0.0);
        } else {
            answer.setCorrect(false);
            answer.setAwardedScore(0.0);
        }
        answers.save(answer);
    }

    private boolean matches(String actual, String expected, boolean caseSensitive) {
        if (actual == null)
            return false;
        String left = actual.strip();
        String right = expected.strip();
        return caseSensitive ? left.equals(right) : left.equalsIgnoreCase(right);
    }

    private AssessmentAttempt accessibleAttempt(Long id, UserSummary actor) {
        if (actor == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        if (actor.role() == Role.STUDENT)
            return attempts.findByIdAndStudentId(id, actor.id()).orElseThrow(() -> forbidden());
        if (actor.role() == Role.ADMIN)
            return attempts.findById(id).orElseThrow(() -> notFound());
        if (actor.role() == Role.TEACHER) {
            AssessmentAttempt item = attempts.findById(id).orElseThrow(() -> notFound());
            assessments.ownedAssessment(item.getAssessment().getId(), actor);
            return item;
        }
        throw forbidden();
    }

    private void requireStudent(UserSummary actor) {
        if (actor == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        if (actor.role() != Role.STUDENT)
            throw forbidden();
    }

    private void requireInProgress(AssessmentAttempt attempt, boolean checkDeadline) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài làm đã được nộp.");
        Integer minutes = attempt.getAssessment().getDurationMinutes();
        if (checkDeadline && minutes != null
                && attempt.getStartedAt().plusSeconds(minutes * 60L).isBefore(Instant.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đã hết thời gian làm bài. Hãy nộp bài để xem kết quả.");
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài làm.");
    }
}
