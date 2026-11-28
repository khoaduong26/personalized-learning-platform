package com.tlcn.backend.exercise.service.impl;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.exercise.generation.ExerciseQuestionGenerator;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import com.tlcn.backend.exercise.service.ExerciseGenerationResult;
import com.tlcn.backend.exercise.service.ExerciseService;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.notification.NotificationService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Locale;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ExerciseServiceImpl implements ExerciseService {
    private static final Logger log = LoggerFactory.getLogger(ExerciseServiceImpl.class);
    private final StepExerciseRepository exercises;
    private final QuestionRepository questions;
    private final LearningStepRepository steps;
    private final LessonRepository lessons;
    private final ExerciseQuestionGenerator generator;
    private final Validator validator;
    private final NotificationService notifications;

    public ExerciseServiceImpl(StepExerciseRepository exercises, QuestionRepository questions,
            LearningStepRepository steps, LessonRepository lessons, ExerciseQuestionGenerator generator,
            Validator validator, NotificationService notifications) {
        this.exercises = exercises;
        this.questions = questions;
        this.steps = steps;
        this.lessons = lessons;
        this.generator = generator;
        this.validator = validator;
        this.notifications = notifications;
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
    public List<StepExercise> pendingReview(UserSummary actor) {
        requireExerciseReviewer(actor);
        return exercises.findPendingReviewForTeacher(actor.id());
    }

    @Transactional
    public Question createGenerated(Long stepId, QuestionRequest request, UserSummary actor) {
        requireTeacher(actor);
        LearningStep step = getStep(stepId);
        requireCanManageStep(step, actor);
        if (!step.getLesson().getId().equals(request.lessonId()))
            throw badRequest("Bài tập phải gắn với bài học của bước đang chọn.");

        return savePendingExercise(step, request);
    }

    @Transactional
    public Question createManual(Long stepId, QuestionRequest request, UserSummary actor) {
        requireTeacher(actor);
        LearningStep step = getStep(stepId);
        requireCanManageStep(step, actor);
        if (!step.getLesson().getId().equals(request.lessonId()))
            throw badRequest("Bài tập phải gắn với bài học của bước đang chọn.");

        Question question = buildQuestion(request);
        question.setAiGenerated(false);
        question.setApprovalStatus(ApprovalStatus.PENDING);
        question = questions.save(question);
        StepExercise exercise = new StepExercise();
        exercise.setStep(step);
        exercise.setQuestion(question);
        exercises.save(exercise);
        return question;
    }

    @Transactional
    public ExerciseGenerationResult generateForStep(Long stepId, UserSummary actor) {
        requireTeacher(actor);
        LearningStep step = getStep(stepId);
        requireCanManageStep(step, actor);

        ExerciseQuestionGenerator.GeneratedExercise generated;
        try {
            generated = generator.generate(step);
        } catch (RuntimeException exception) {
            log.error("Configured exercise generator failed (adapter={}).", generator.getClass().getSimpleName(), exception);
            throw generationFailure("Bộ sinh bài tập hiện không khả dụng. " + describeGeneratorFailure(exception));
        }
        if (generated == null || generated.question() == null || generated.provider() == null
                || generated.provider().isBlank() || (generated.demo() && (generated.notice() == null
                        || generated.notice().isBlank())))
            throw generationFailure("Bộ sinh bài tập trả về kết quả không hợp lệ.");

        QuestionRequest request = generated.question();
        var violations = validator.validate(request);
        if (!violations.isEmpty())
            throw generationFailure("Bộ sinh bài tập trả về dữ liệu không hợp lệ: "
                    + violations.iterator().next().getMessage());
        if (!step.getLesson().getId().equals(request.lessonId()))
            throw generationFailure("Bộ sinh bài tập trả về bài học không khớp với bước đang chọn.");

        try {
            Question question = savePendingExercise(step, request);
            notifications.notifyAfterCommit(actor.id(), "AI_EXERCISE_READY", "Bài tập AI đã sẵn sàng",
                    "Bài tập cho " + step.getLesson().getName() + " đang chờ thầy cô duyệt.", "/exercises/review");
            return new ExerciseGenerationResult(question, generated.provider(), generated.demo(), generated.notice());
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() == HttpStatus.BAD_REQUEST || exception.getStatusCode() == HttpStatus.NOT_FOUND)
                throw generationFailure("Không thể lưu đầu ra của bộ sinh bài tập: " + exception.getReason());
            throw exception;
        }
    }

    @Transactional
    public Question updateGenerated(Long questionId, QuestionRequest request, UserSummary actor) {
        requireExerciseReviewer(actor);
        Question question = getReviewableQuestion(questionId, actor);
        if (question.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể sửa bài tập đang chờ duyệt.");
        if (!question.getLesson().getId().equals(request.lessonId()))
            throw badRequest("Không thể đổi bài học đã liên kết với bước học.");

        Question replacement = buildQuestion(request);
        Question current = (Question) Hibernate.unproxy(question);
        if (!current.getClass().equals(replacement.getClass()))
            throw badRequest("Không thể đổi dạng câu hỏi khi chỉnh sửa.");
        current.setContent(replacement.getContent());
        current.setDifficulty(replacement.getDifficulty());
        current.setScore(replacement.getScore());
        current.setExplanation(replacement.getExplanation());
        if (current instanceof MultipleChoiceQuestion currentMultiple
                && replacement instanceof MultipleChoiceQuestion nextMultiple) {
            currentMultiple.setAllowMultiple(nextMultiple.isAllowMultiple());
            currentMultiple.setOptions(nextMultiple.getOptions());
        } else if (current instanceof FillBlankQuestion currentFill
                && replacement instanceof FillBlankQuestion nextFill) {
            currentFill.setCaseSensitive(nextFill.isCaseSensitive());
            currentFill.setBlanks(nextFill.getBlanks());
        } else if (current instanceof ShortAnswerQuestion currentShort
                && replacement instanceof ShortAnswerQuestion nextShort) {
            currentShort.setCaseSensitive(nextShort.isCaseSensitive());
            currentShort.setCorrectAnswer(nextShort.getCorrectAnswer());
        }
        return questions.save(current);
    }

    @Transactional
    public Question review(Long questionId, ApprovalStatus decision, UserSummary actor) {
        requireExerciseReviewer(actor);
        if (decision == null || decision == ApprovalStatus.PENDING)
            throw badRequest("Quyết định duyệt phải là APPROVED hoặc REJECTED.");
        Question question = getReviewableQuestion(questionId, actor);
        if (question.getApprovalStatus() != ApprovalStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài tập này đã được xử lý.");
        question.setApprovalStatus(decision);
        Question saved = questions.save(question);
        if (decision == ApprovalStatus.APPROVED) {
            StepExercise exercise = exercises.findByQuestionId(questionId).orElseThrow();
            Long studentId = exercise.getStep().getPath().getStudent().getId();
            notifications.notifyAfterCommit(studentId, "EXERCISE_APPROVED", "Có bài luyện tập mới",
                    "Thầy cô đã duyệt bài tập cho " + exercise.getStep().getLesson().getName() + ".",
                    "/my-learning-path");
        }
        return saved;
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

    private Question savePendingExercise(LearningStep step, QuestionRequest request) {
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

    private ResponseStatusException generationFailure(String message) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, message);
    }

    private String describeGeneratorFailure(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return "Nhà cung cấp AI trả HTTP " + responseException.getStatusCode().value()
                    + ". Hãy kiểm tra cấu hình khóa, model và hạn mức trong backend.";
        }
        if (exception instanceof ResourceAccessException) {
            return "Backend không kết nối được tới nhà cung cấp AI hoặc bị hết thời gian chờ.";
        }
        if (exception instanceof IllegalStateException && exception.getMessage() != null) {
            return exception.getMessage();
        }
        return "Hãy xem log backend để biết chi tiết; nội dung câu hỏi và khóa API không được đưa vào thông báo.";
    }

    private LearningStep getStep(Long stepId) {
        return steps.findById(stepId).orElseThrow(() -> notFound("Không tìm thấy bước học."));
    }

    private void requireCanAccessStep(LearningStep step, UserSummary actor) {
        requireSignedIn(actor);
        if (step.getPath().getStatus() == LearningPathStatus.DELETED) throw notFound("Không tìm thấy bước học.");
        if (actor.role() == Role.ADMIN)
            return;
        if (actor.role() == Role.STUDENT && step.getPath().getStudent().getId().equals(actor.id())) {
            if (step.getStatus() != LearningStepStatus.IN_PROGRESS)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bước học này chưa được mở hoặc đã hoàn thành.");
            return;
        }
        if (actor.role() == Role.TEACHER && ownsStudent(step, actor))
            return;
        throw forbidden();
    }

    private void requireCanManageStep(LearningStep step, UserSummary actor) {
        requireTeacher(actor);
        if (step.getPath().getStatus() == LearningPathStatus.DELETED) throw notFound("Không tìm thấy bước học.");
        if (actor.role() == Role.ADMIN || ownsStudent(step, actor))
            return;
        throw forbidden();
    }

    private boolean ownsStudent(LearningStep step, UserSummary teacher) {
        var classroom = step.getPath().getClassroom();
        return classroom != null && classroom.getTeacher().getId().equals(teacher.id());
    }

    private void requireTeacher(UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() != Role.ADMIN && actor.role() != Role.TEACHER)
            throw forbidden();
    }

    private void requireExerciseReviewer(UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() != Role.TEACHER)
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
