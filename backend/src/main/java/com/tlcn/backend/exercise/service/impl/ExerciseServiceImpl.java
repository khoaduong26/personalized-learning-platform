package com.tlcn.backend.exercise.service.impl;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import com.tlcn.backend.exercise.service.ExerciseService;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExerciseServiceImpl implements ExerciseService {
    private final StepExerciseRepository exercises;
    private final QuestionRepository questions;
    private final LearningStepRepository steps;
    private final LessonRepository lessons;

    public ExerciseServiceImpl(StepExerciseRepository exercises, QuestionRepository questions,
            LearningStepRepository steps, LessonRepository lessons) {
        this.exercises = exercises;
        this.questions = questions;
        this.steps = steps;
        this.lessons = lessons;
    }

    @Transactional(readOnly = true)
    public List<StepExercise> forStep(Long stepId, UserSummary actor) {
        LearningStep step = getStep(stepId);
        requireCanAccessStep(step, actor);
        List<StepExercise> attached = exercises.findByStepId(stepId);
        if (actor.role() == Role.STUDENT)
            return attached.stream()
                    .filter(e -> e.getQuestion().getApprovalStatus() == ApprovalStatus.APPROVED).toList();
        return attached;
    }

    @Transactional(readOnly = true)
    public List<Question> pendingReview(UserSummary actor) {
        requireTeacher(actor);
        return actor.role() == Role.ADMIN ? exercises.findAllPendingReview()
                : exercises.findPendingReviewForTeacher(actor.id());
    }

    @Transactional
    public Question createGenerated(Long stepId, QuestionRequest request, UserSummary actor) {
        requireTeacher(actor);
        LearningStep step = getStep(stepId);
        requireCanManageStep(step, actor);
        if (!step.getLesson().getId().equals(request.lessonId()))
            throw badRequest("Bài tập phải gắn với bài học của bước đang chọn.");

        Question question = buildQuestion(request);
        question.setAiGenerated(true);
        question.setApprovalStatus(ApprovalStatus.PENDING);
        question = questions.save(question);
        StepExercise exercise = new StepExercise();
        exercise.setStep(step);
        exercise.setQuestion(question);
        exercises.save(exercise);
        return question;
    }

    @Transactional
    public Question updateGenerated(Long questionId, QuestionRequest request, UserSummary actor) {
        requireTeacher(actor);
        Question question = getReviewableQuestion(questionId, actor);
        if (question.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể sửa bài tập đang chờ duyệt.");
        if (!question.getLesson().getId().equals(request.lessonId()))
            throw badRequest("Không thể đổi bài học đã liên kết với bước học.");

        Question replacement = buildQuestion(request);
        if (!question.getClass().equals(replacement.getClass()))
            throw badRequest("Không thể đổi dạng câu hỏi khi chỉnh sửa.");
        question.setContent(replacement.getContent());
        question.setDifficulty(replacement.getDifficulty());
        question.setScore(replacement.getScore());
        question.setExplanation(replacement.getExplanation());
        if (question instanceof MultipleChoiceQuestion currentMultiple
                && replacement instanceof MultipleChoiceQuestion nextMultiple) {
            currentMultiple.setAllowMultiple(nextMultiple.isAllowMultiple());
            currentMultiple.setOptions(nextMultiple.getOptions());
        } else if (question instanceof FillBlankQuestion currentFill
                && replacement instanceof FillBlankQuestion nextFill) {
            currentFill.setCaseSensitive(nextFill.isCaseSensitive());
            currentFill.setBlanks(nextFill.getBlanks());
        } else if (question instanceof ShortAnswerQuestion currentShort
                && replacement instanceof ShortAnswerQuestion nextShort) {
            currentShort.setCaseSensitive(nextShort.isCaseSensitive());
            currentShort.setCorrectAnswer(nextShort.getCorrectAnswer());
        }
        return questions.save(question);
    }

    @Transactional
    public Question review(Long questionId, ApprovalStatus decision, UserSummary actor) {
        requireTeacher(actor);
        if (decision == null || decision == ApprovalStatus.PENDING)
            throw badRequest("Quyết định duyệt phải là APPROVED hoặc REJECTED.");
        Question question = getReviewableQuestion(questionId, actor);
        if (question.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài tập này đã được xử lý.");
        question.setApprovalStatus(decision);
        return questions.save(question);
    }

    @Transactional
    public StepExercise attach(StepExercise exercise) {
        exercise.getQuestion().setAiGenerated(true);
        exercise.getQuestion().setApprovalStatus(ApprovalStatus.PENDING);
        return exercises.save(exercise);
    }

    private Question getReviewableQuestion(Long questionId, UserSummary actor) {
        StepExercise exercise = exercises.findByQuestionId(questionId)
                .orElseThrow(() -> notFound("Không tìm thấy bài tập."));
        requireCanManageStep(exercise.getStep(), actor);
        Question question = exercise.getQuestion();
        if (!question.isAiGenerated())
            throw badRequest("Chỉ có thể duyệt bài tập được sinh tự động.");
        return question;
    }

    private Question buildQuestion(QuestionRequest request) {
        Lesson lesson = lessons.findById(request.lessonId())
                .orElseThrow(() -> notFound("Không tìm thấy bài học."));
        String kind = request.kind().toUpperCase(Locale.ROOT);
        Question question;
        switch (kind) {
            case "MULTIPLE_CHOICE" -> {
                if (request.options() == null || request.options().size() < 2
                        || request.options().stream().filter(QuestionRequest.OptionInput::correct).count() == 0)
                    throw badRequest("Câu trắc nghiệm cần ít nhất hai lựa chọn và một đáp án đúng.");
                boolean allowMultiple = Boolean.TRUE.equals(request.allowMultiple());
                if (!allowMultiple && request.options().stream()
                        .filter(QuestionRequest.OptionInput::correct).count() != 1)
                    throw badRequest("Câu hỏi một đáp án chỉ được có một lựa chọn đúng.");
                MultipleChoiceQuestion value = new MultipleChoiceQuestion();
                value.setAllowMultiple(allowMultiple);
                value.setOptions(request.options().stream().map(input -> {
                    Option option = new Option();
                    option.setName(input.name().strip());
                    option.setCorrect(input.correct());
                    option.setDisplayOrder(input.displayOrder());
                    return option;
                }).toList());
                question = value;
            }
            case "FILL_BLANK" -> {
                if (request.blanks() == null || request.blanks().isEmpty())
                    throw badRequest("Cần ít nhất một ô trống và đáp án.");
                FillBlankQuestion value = new FillBlankQuestion();
                value.setCaseSensitive(Boolean.TRUE.equals(request.caseSensitive()));
                value.setBlanks(request.blanks().stream().map(input -> {
                    Blank blank = new Blank();
                    blank.setCorrectAnswer(input.correctAnswer().strip());
                    blank.setPosition(input.position());
                    return blank;
                }).toList());
                question = value;
            }
            case "SHORT_ANSWER" -> {
                if (request.correctAnswer() == null || request.correctAnswer().isBlank())
                    throw badRequest("Cần nhập đáp án chuẩn.");
                ShortAnswerQuestion value = new ShortAnswerQuestion();
                value.setCaseSensitive(Boolean.TRUE.equals(request.caseSensitive()));
                value.setCorrectAnswer(request.correctAnswer().strip());
                question = value;
            }
            default -> throw badRequest("Dạng câu hỏi không được hỗ trợ.");
        }
        question.setContent(request.content().strip());
        question.setDifficulty(request.difficulty());
        question.setScore(request.score());
        question.setLesson(lesson);
        question.setAiGenerated(true);
        question.setApprovalStatus(ApprovalStatus.PENDING);
        question.setExplanation(request.explanation());
        return question;
    }

    private LearningStep getStep(Long stepId) {
        return steps.findById(stepId).orElseThrow(() -> notFound("Không tìm thấy bước học."));
    }

    private void requireCanAccessStep(LearningStep step, UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() == Role.ADMIN)
            return;
        if (actor.role() == Role.STUDENT && step.getPath().getStudent().getId().equals(actor.id()))
            return;
        if (actor.role() == Role.TEACHER && ownsStudent(step, actor))
            return;
        throw forbidden();
    }

    private void requireCanManageStep(LearningStep step, UserSummary actor) {
        requireTeacher(actor);
        if (actor.role() == Role.ADMIN || ownsStudent(step, actor))
            return;
        throw forbidden();
    }

    private boolean ownsStudent(LearningStep step, UserSummary teacher) {
        var student = step.getPath().getStudent();
        return student.getClassroom() != null
                && student.getClassroom().getTeacher().getId().equals(teacher.id());
    }

    private void requireTeacher(UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() != Role.ADMIN && actor.role() != Role.TEACHER)
            throw forbidden();
    }

    private void requireSignedIn(UserSummary actor) {
        if (actor == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập dữ liệu này.");
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
