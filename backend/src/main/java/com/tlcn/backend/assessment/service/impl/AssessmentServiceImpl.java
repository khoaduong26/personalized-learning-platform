package com.tlcn.backend.assessment.service.impl;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.assessment.dto.AssessmentRequest;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.dto.QuestionEditorView;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.*;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.TeacherRepository;
import com.tlcn.backend.user.repository.StudentRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssessmentServiceImpl implements AssessmentService {
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AssessmentAttemptRepository attempts;
    private final LessonRepository lessons;
    private final TeacherRepository teachers;
    private final ClassroomRepository classrooms;
    private final StudentRepository students;

    public AssessmentServiceImpl(AssessmentRepository assessments, QuestionRepository questions,
            AssessmentAttemptRepository attempts, LessonRepository lessons, TeacherRepository teachers,
            MultipleChoiceQuestionRepository multipleChoiceQuestions, FillBlankQuestionRepository fillBlankQuestions,
            ShortAnswerQuestionRepository shortAnswerQuestions, ClassroomRepository classrooms,
            StudentRepository students) {
        this.assessments = assessments;
        this.questions = questions;
        this.attempts = attempts;
        this.lessons = lessons;
        this.teachers = teachers;
        this.classrooms = classrooms;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<Assessment> list(UserSummary actor) {
        requireSignedIn(actor);
        List<Assessment> values;
        if (actor.role() == Role.STUDENT)
            values = assessments.findPublishedForStudent(actor.id());
        else if (actor.role() == Role.ADMIN)
            values = assessments.findAll();
        else if (actor.role() == Role.TEACHER)
            values = assessments.findByTeacherIdOrderByCreatedAtDesc(actor.id());
        else
            throw forbidden();
        values.forEach(this::initializeAssessmentView);
        return values;
    }

    @Transactional(readOnly = true)
    public Assessment get(Long id, UserSummary actor) {
        Assessment value = assessments.findById(id).orElseThrow(() -> notFound("Không tìm thấy bài kiểm tra."));
        requireCanView(value, actor);
        initializeAssessmentView(value);
        return value;
    }

    @Transactional
    public Assessment save(AssessmentRequest request, UserSummary actor) {
        requireTeacher(actor);
        Teacher teacher = teachers.findById(actor.id()).orElseThrow(() -> forbidden());
        List<Question> attached = questions.findAllById(request.questionIds());
        if (attached.size() != new HashSet<>(request.questionIds()).size())
            throw notFound("Một số câu hỏi không tồn tại.");
        if (attached.stream().anyMatch(question -> !question.isActive()))
            throw badRequest("Một số câu hỏi không còn trong ngân hàng.");
        if (attached.stream().anyMatch(question -> question.isAiGenerated()
                && question.getApprovalStatus() != ApprovalStatus.APPROVED))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ có thể giao bài tập AI đã được giáo viên duyệt.");
        if (actor.role() == Role.TEACHER && attached.stream().anyMatch(question -> !canUseLesson(question.getLesson(), actor)))
            throw forbidden();
        List<Classroom> assignedClassrooms = classrooms.findAllById(request.classroomIds());
        if (assignedClassrooms.size() != new HashSet<>(request.classroomIds()).size())
            throw notFound("Một số lớp học không tồn tại.");
        if (actor.role() == Role.TEACHER && assignedClassrooms.stream()
                .anyMatch(classroom -> !classroom.getTeacher().getId().equals(actor.id())))
            throw forbidden();
        Assessment value = new Assessment();
        value.setTitle(request.title().strip());
        value.setType(request.type());
        value.setDurationMinutes(request.durationMinutes());
        value.setTeacher(teacher);
        value.setQuestions(attached);
        value.setClassrooms(assignedClassrooms);
        initializeAssessmentView(value);
        return assessments.save(value);
    }

    @Transactional
    public Assessment publish(Long id, UserSummary actor) {
        Assessment value = ownedAssessment(id, actor);
        if (value.getQuestions().isEmpty())
            throw badRequest("Hãy thêm ít nhất một câu hỏi trước khi giao bài.");
        if (value.getClassrooms().isEmpty())
            throw badRequest("Hãy chọn ít nhất một lớp trước khi giao bài.");
        value.setPublished(true);
        initializeAssessmentView(value);
        return value;
    }

    @Transactional(readOnly = true)
    public List<Question> questionsForLesson(Long lessonId, UserSummary actor) {
        requireTeacher(actor);
        Lesson lesson = lessons.findById(lessonId).orElseThrow(() -> notFound("Không tìm thấy bài học."));
        requireCanManageQuestion(lesson, actor);
        List<Question> found = questions.findActiveByLessonIdOrderByIdDesc(lessonId);
        found.forEach(this::initializeQuestionView);
        return found;
    }

    @Transactional
    public Question createQuestion(QuestionRequest request, UserSummary actor) {
        requireTeacher(actor);
        Lesson lesson = lessons.findById(request.lessonId()).orElseThrow(() -> notFound("Không tìm thấy bài học."));
        requireCanManageQuestion(lesson, actor);
        String kind = request.kind().toUpperCase(Locale.ROOT);
        Question q;
        switch (kind) {
            case "MULTIPLE_CHOICE" -> {
                if (request.options() == null || request.options().size() < 2
                        || request.options().stream().filter(QuestionRequest.OptionInput::correct).count() == 0)
                    throw badRequest("Câu trắc nghiệm cần ít nhất hai lựa chọn và một đáp án đúng.");
                boolean multi = Boolean.TRUE.equals(request.allowMultiple());
                if (!multi && request.options().stream().filter(QuestionRequest.OptionInput::correct).count() != 1)
                    throw badRequest("Câu hỏi một đáp án chỉ được có một lựa chọn đúng.");
                MultipleChoiceQuestion value = new MultipleChoiceQuestion();
                value.setAllowMultiple(multi);
                value.setOptions(request.options().stream().map(input -> {
                    Option option = new Option();
                    option.setName(input.name().strip());
                    option.setCorrect(input.correct());
                    option.setDisplayOrder(input.displayOrder());
                    return option;
                }).toList());
                q = value;
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
                q = value;
            }
            case "SHORT_ANSWER" -> {
                if (request.correctAnswer() == null || request.correctAnswer().isBlank())
                    throw badRequest("Cần nhập đáp án chuẩn.");
                ShortAnswerQuestion value = new ShortAnswerQuestion();
                value.setCaseSensitive(Boolean.TRUE.equals(request.caseSensitive()));
                value.setCorrectAnswer(request.correctAnswer().strip());
                q = value;
            }
            default -> throw badRequest("Dạng câu hỏi không được hỗ trợ.");
        }
        q.setContent(request.content().strip());
        q.setDifficulty(request.difficulty());
        q.setScore(request.score());
        q.setLesson(lesson);
        q.setAiGenerated(false);
        q.setApprovalStatus(ApprovalStatus.APPROVED);
        q.setExplanation(request.explanation());
        initializeQuestionView(q);
        return questions.save(q);
    }

    @Transactional(readOnly = true)
    public QuestionEditorView questionForEditing(Long questionId, UserSummary actor) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> notFound("Không tìm thấy câu hỏi."));
        requireCanManageQuestion(question, actor);
        if (!question.isActive()) throw notFound("Câu hỏi không còn trong ngân hàng.");
        initializeQuestionView(question);
        return QuestionEditorView.from(question);
    }

    @Transactional
    public Question updateQuestion(Long questionId, QuestionRequest request, UserSummary actor) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> notFound("Không tìm thấy câu hỏi."));
        requireCanManageQuestion(question, actor);
        if (!question.isActive()) throw notFound("Câu hỏi không còn trong ngân hàng.");
        Lesson lesson = lessons.findById(request.lessonId())
                .orElseThrow(() -> notFound("Không tìm thấy bài học."));
        requireCanManageQuestion(lesson, actor);

        String requestedKind = request.kind().toUpperCase(Locale.ROOT);
        if (!questionKind(question).equals(requestedKind) || isQuestionInUse(questionId)) {
            // Keep every assessment and student answer attached to the unchanged version.
            question.setActive(false);
            questions.save(question);
            Question nextVersion = newQuestion(requestedKind);
            nextVersion.setAiGenerated(question.isAiGenerated());
            nextVersion.setApprovalStatus(question.getApprovalStatus());
            question = nextVersion;
        }
        applyQuestionFields(question, request, lesson);
        initializeQuestionView(question);
        return questions.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId, UserSummary actor) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> notFound("Không tìm thấy câu hỏi."));
        requireCanManageQuestion(question, actor);
        if (!question.isActive()) throw notFound("Câu hỏi không còn trong ngân hàng.");
        question.setActive(false);
        questions.save(question);
    }

    private boolean isQuestionInUse(Long questionId) {
        return assessments.existsByQuestions_Id(questionId) || attempts.existsByAnswers_Question_Id(questionId);
    }

    private Question newQuestion(String kind) {
        return switch (kind) {
            case "MULTIPLE_CHOICE" -> new MultipleChoiceQuestion();
            case "FILL_BLANK" -> new FillBlankQuestion();
            case "SHORT_ANSWER" -> new ShortAnswerQuestion();
            default -> throw badRequest("Dạng câu hỏi không được hỗ trợ.");
        };
    }

    private String questionKind(Question question) {
        return question instanceof MultipleChoiceQuestion ? "MULTIPLE_CHOICE"
                : question instanceof FillBlankQuestion ? "FILL_BLANK" : "SHORT_ANSWER";
    }

    private void requireCanManageQuestion(Question question, UserSummary actor) {
        requireCanManageQuestion(question.getLesson(), actor);
    }

    private void requireCanManageQuestion(Lesson lesson, UserSummary actor) {
        requireSignedIn(actor);
        User owner = lesson.getTopic().getSubject().getOwner();
        boolean allowed = actor.role() == Role.ADMIN ? owner == null
                : actor.role() == Role.TEACHER && owner != null && owner.getId().equals(actor.id());
        if (!allowed) throw forbidden();
    }

    private void applyQuestionFields(Question q, QuestionRequest request, Lesson lesson) {
        String kind = request.kind().toUpperCase(Locale.ROOT);
        switch (kind) {
            case "MULTIPLE_CHOICE" -> {
                if (request.options() == null || request.options().size() < 2
                        || request.options().stream().filter(QuestionRequest.OptionInput::correct).count() == 0)
                    throw badRequest("Câu trắc nghiệm cần ít nhất hai lựa chọn và một đáp án đúng.");
                boolean multi = Boolean.TRUE.equals(request.allowMultiple());
                if (!multi && request.options().stream().filter(QuestionRequest.OptionInput::correct).count() != 1)
                    throw badRequest("Câu hỏi một đáp án chỉ được có một lựa chọn đúng.");
                MultipleChoiceQuestion value = (MultipleChoiceQuestion) q;
                value.setAllowMultiple(multi);
                value.setOptions(request.options().stream().map(input -> {
                    Option option = new Option();
                    option.setName(input.name().strip());
                    option.setCorrect(input.correct());
                    option.setDisplayOrder(input.displayOrder());
                    return option;
                }).toList());
            }
            case "FILL_BLANK" -> {
                if (request.blanks() == null || request.blanks().isEmpty())
                    throw badRequest("Cần ít nhất một ô trống và đáp án.");
                FillBlankQuestion value = (FillBlankQuestion) q;
                value.setCaseSensitive(Boolean.TRUE.equals(request.caseSensitive()));
                value.setBlanks(request.blanks().stream().map(input -> {
                    Blank blank = new Blank();
                    blank.setCorrectAnswer(input.correctAnswer().strip());
                    blank.setPosition(input.position());
                    return blank;
                }).toList());
            }
            case "SHORT_ANSWER" -> {
                if (request.correctAnswer() == null || request.correctAnswer().isBlank())
                    throw badRequest("Cần nhập đáp án chuẩn.");
                ShortAnswerQuestion value = (ShortAnswerQuestion) q;
                value.setCaseSensitive(Boolean.TRUE.equals(request.caseSensitive()));
                value.setCorrectAnswer(request.correctAnswer().strip());
            }
            default -> throw badRequest("Dạng câu hỏi không được hỗ trợ.");
        }
        q.setContent(request.content().strip());
        q.setDifficulty(request.difficulty());
        q.setScore(request.score());
        q.setLesson(lesson);
        q.setExplanation(request.explanation());
    }

    @Transactional(readOnly = true)
    public List<AssessmentAttempt> attemptsForStudent(Long studentId, Long requestedClassroomId, UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() == Role.STUDENT && !actor.id().equals(studentId))
            throw forbidden();
        if (actor.role() != Role.STUDENT && actor.role() != Role.TEACHER && actor.role() != Role.ADMIN)
            throw forbidden();
        List<Classroom> teacherClassrooms = List.of();
        if (actor.role() == Role.TEACHER) {
            var student = students.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh."));
            teacherClassrooms = student.getClassrooms().stream()
                    .filter(classroom -> classroom.getTeacher().getId().equals(actor.id())).toList();
            if (requestedClassroomId != null)
                teacherClassrooms = teacherClassrooms.stream()
                        .filter(classroom -> classroom.getId().equals(requestedClassroomId)).toList();
            if (teacherClassrooms.isEmpty())
                throw forbidden();
        } else if (actor.role() == Role.ADMIN && requestedClassroomId != null) {
            var student = students.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh."));
            teacherClassrooms = student.getClassrooms().stream()
                    .filter(classroom -> classroom.getId().equals(requestedClassroomId)).toList();
            if (teacherClassrooms.isEmpty()) throw forbidden();
        }
        List<AssessmentAttempt> found = attempts.findByStudentIdOrderByStartedAtDesc(studentId);
        if (actor.role() == Role.TEACHER || (actor.role() == Role.ADMIN && requestedClassroomId != null)) {
            List<Long> classroomIds = teacherClassrooms.stream().map(Classroom::getId).toList();
            found = found.stream().filter(attempt -> {
                if (attempt.getLearningStep() != null)
                    return attempt.getLearningStep().getPath().getClassroom() != null
                            && classroomIds.contains(attempt.getLearningStep().getPath().getClassroom().getId());
                return attempt.getAssessment() != null && attempt.getAssessment().getClassrooms().stream()
                        .anyMatch(assigned -> classroomIds.contains(assigned.getId()));
            }).toList();
        }
        return found;
    }

    @Transactional(readOnly = true)
    public List<Classroom> classroomsForTeacher(UserSummary actor) {
        requireTeacher(actor);
        return classrooms.findByTeacherIdOrderByNameAsc(actor.id());
    }

    public Assessment ownedAssessment(Long id, UserSummary actor) {
        requireTeacher(actor);
        if (actor.role() == Role.ADMIN)
            return assessments.findById(id).orElseThrow(() -> notFound("Không tìm thấy bài kiểm tra."));
        return assessments.findByIdAndTeacherId(id, actor.id()).orElseThrow(() -> forbidden());
    }

    private void requireCanView(Assessment value, UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() == Role.ADMIN
                || (actor.role() == Role.TEACHER && value.getTeacher().getId().equals(actor.id())))
            return;
        if (actor.role() == Role.STUDENT && value.isPublished()
                && value.getClassrooms().stream().anyMatch(classroom -> classroom.getStudents().stream()
                        .anyMatch(student -> student.getId().equals(actor.id()))))
            return;
        throw forbidden();
    }

    private void initializeAssessmentView(Assessment value) {
        value.getTeacher().getFullName();
        value.getClassrooms().forEach(classroom -> {
            classroom.getId();
            classroom.getName();
        });
        value.getQuestions().forEach(this::initializeQuestionView);
    }

    private void initializeQuestionView(Question question) {
        question.getLesson().getId();
        if (question instanceof MultipleChoiceQuestion multipleChoice)
            multipleChoice.getOptions().forEach(option -> {
                option.getId();
                option.getName();
                option.getDisplayOrder();
            });
        if (question instanceof FillBlankQuestion fillBlank)
            fillBlank.getBlanks().size();
    }

    private void requireTeacher(UserSummary actor) {
        requireSignedIn(actor);
        if (actor.role() != Role.TEACHER && actor.role() != Role.ADMIN)
            throw forbidden();
    }

    private void requireCanUseLesson(Lesson lesson, UserSummary actor) {
        if (!canUseLesson(lesson, actor)) throw forbidden();
    }

    private boolean canUseLesson(Lesson lesson, UserSummary actor) {
        User owner = lesson.getTopic().getSubject().getOwner();
        return owner == null || actor.role() == Role.ADMIN || owner.getId().equals(actor.id());
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
