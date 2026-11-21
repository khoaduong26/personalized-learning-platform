package com.tlcn.backend.dev;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.AssessmentAttemptRepository;
import com.tlcn.backend.assessment.repository.AssessmentRepository;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.repository.LearningPathRepository;
import com.tlcn.backend.mastery.entity.*;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.MasteryHistoryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.StudentRepository;
import com.tlcn.backend.user.repository.TeacherRepository;
import com.tlcn.backend.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synthetic end-to-end data for a local development database only.
 * It is disabled unless both the "demo" profile and the explicit enable flag are set.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(prefix = "app.demo-seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    public static final String DEMO_PASSWORD = "Demo123!";
    private static final String TEACHER_EMAIL = "teacher.demo@example.test";
    private static final String STUDENT_AN_EMAIL = "student.an@example.test";
    private static final String STUDENT_BINH_EMAIL = "student.binh@example.test";
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final TeacherRepository teachers;
    private final StudentRepository students;
    private final ClassroomRepository classrooms;
    private final SubjectRepository subjects;
    private final TopicRepository topics;
    private final LessonRepository lessons;
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AssessmentAttemptRepository attempts;
    private final KnowledgeMasteryRepository mastery;
    private final MasteryHistoryRepository masteryHistory;
    private final KnowledgeGapRepository gaps;
    private final StudentErrorRepository studentErrors;
    private final LearningPathRepository paths;
    private final StepExerciseRepository exercises;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, TeacherRepository teachers, StudentRepository students,
            ClassroomRepository classrooms, SubjectRepository subjects, TopicRepository topics,
            LessonRepository lessons, AssessmentRepository assessments, QuestionRepository questions,
            AssessmentAttemptRepository attempts, KnowledgeMasteryRepository mastery,
            MasteryHistoryRepository masteryHistory, KnowledgeGapRepository gaps,
            StudentErrorRepository studentErrors, LearningPathRepository paths,
            StepExerciseRepository exercises, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.teachers = teachers;
        this.students = students;
        this.classrooms = classrooms;
        this.subjects = subjects;
        this.topics = topics;
        this.lessons = lessons;
        this.assessments = assessments;
        this.questions = questions;
        this.attempts = attempts;
        this.mastery = mastery;
        this.masteryHistory = masteryHistory;
        this.gaps = gaps;
        this.studentErrors = studentErrors;
        this.paths = paths;
        this.exercises = exercises;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByEmail(TEACHER_EMAIL).isPresent()) {
            log.info("Demo records already exist; skipping seed.");
            return;
        }

        Teacher teacher = new Teacher();
        setAccount(teacher, "Cô Mai Demo", TEACHER_EMAIL);
        teacher = teachers.save(teacher);

        Student an = new Student();
        setAccount(an, "Bạn An Demo", STUDENT_AN_EMAIL);
        an.setSex("FEMALE");
        an.setDateOfBirth(LocalDate.of(2018, 4, 12));
        an = students.save(an);

        Student binh = new Student();
        setAccount(binh, "Bạn Bình Demo", STUDENT_BINH_EMAIL);
        binh.setSex("MALE");
        binh.setDateOfBirth(LocalDate.of(2018, 9, 3));
        binh = students.save(binh);

        Classroom classroom = classrooms.save(new Classroom("Lớp 2A - Demo", "2026-2027", teacher));
        an.setClassroom(classroom);
        binh.setClassroom(classroom);
        students.saveAll(List.of(an, binh));

        Subject subject = subjects.save(new Subject("Toán 2 - Demo"));
        Topic topic = topics.save(new Topic("Phép cộng và phép trừ trong phạm vi 100", subject));
        Lesson addition = lessons.save(new Lesson("Cộng có nhớ trong phạm vi 100",
                "Đặt tính theo cột, cộng hàng đơn vị trước và nhớ sang hàng chục.", topic));
        Lesson subtraction = lessons.save(new Lesson("Trừ có nhớ trong phạm vi 100",
                "Mượn một chục khi hàng đơn vị chưa đủ để trừ.", topic));
        Topic wordProblems = topics.save(new Topic("Giải toán có lời văn", subject));
        Lesson wordProblem = lessons.save(new Lesson("Bài toán thêm bớt một số đơn vị",
                "Đọc đề, xác định dữ kiện và chọn phép tính phù hợp.", wordProblems));

        MultipleChoiceQuestion q1 = multipleChoice(addition, "Tính 27 + 15 = ?", "42", "32", "41");
        ShortAnswerQuestion q2 = shortAnswer(subtraction, "Tính 54 - 28 = ?", "26", false);
        ShortAnswerQuestion q3 = shortAnswer(wordProblem,
                "Lan có 18 nhãn vở, được tặng thêm 7 nhãn. Lan có tất cả bao nhiêu nhãn vở?", "25", false);
        Assessment assessment = new Assessment();
        assessment.setTitle("Khảo sát đầu năm - Demo");
        assessment.setType(AssessmentType.DIAGNOSTIC);
        assessment.setDurationMinutes(20);
        assessment.setTeacher(teacher);
        assessment.setClassrooms(List.of(classroom));
        assessment.setQuestions(List.of(q1, q2, q3));
        assessment.setPublished(true);
        assessment = assessments.save(assessment);

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessment(assessment);
        attempt.setStudent(an);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.addAnswer(answer(q1, true, 5, "42", q1.getOptions().get(0)));
        attempt.addAnswer(answer(q2, false, 0, "34", null));
        attempt.addAnswer(answer(q3, true, 5, "25", null));
        attempt = attempts.save(attempt);

        KnowledgeMastery anAddition = addMastery(an, addition, 82, MasteryLevel.PROFICIENT, 0.8);
        addMastery(an, subtraction, 62, MasteryLevel.DEVELOPING, 0.6);
        addMastery(an, wordProblem, 90, MasteryLevel.MASTERED, 0.8);
        KnowledgeMastery binhAddition = addMastery(binh, addition, 38, MasteryLevel.BEGINNING, 0.4);
        KnowledgeMastery binhSubtraction = addMastery(binh, subtraction, 55, MasteryLevel.DEVELOPING, 0.4);

        addGap(an, subtraction, GapPriority.MEDIUM,
                "Còn nhầm thao tác mượn khi trừ qua chục.", GapStatus.IDENTIFIED);
        addGap(binh, addition, GapPriority.HIGH,
                "Chưa cộng phần nhớ vào hàng chục sau khi cộng hàng đơn vị.", GapStatus.IN_REMEDIATION);
        addGap(binh, subtraction, GapPriority.MEDIUM,
                "Cần thực hành mượn một chục khi trừ qua chục.", GapStatus.IN_REMEDIATION);

        MasteryHistory history = new MasteryHistory();
        history.setMastery(binhAddition);
        history.setOldScore(25.0);
        history.setNewScore(38.0);
        history.setTriggerSource("DEMO_DIAGNOSTIC");
        masteryHistory.save(history);

        StudentAnswer wrongSubtractionAnswer = attempt.getAnswers().get(1);
        StudentError error = new StudentError();
        error.setStudent(an);
        error.setLesson(subtraction);
        error.setAnswer(wrongSubtractionAnswer);
        error.setErrorType("Quên mượn khi trừ qua chục");
        error.setDescription("Em trừ hàng chục trực tiếp mà chưa mượn một chục cho hàng đơn vị.");
        studentErrors.save(error);

        LearningPath path = new LearningPath();
        path.setStudent(binh);
        path.setVersion(1);
        path.setUpdatedAt(Instant.now());
        LearningStep firstStep = step(path, 1, LearningStepStatus.IN_PROGRESS, addition);
        LearningStep secondStep = step(path, 2, LearningStepStatus.LOCKED, subtraction);
        path.getSteps().addAll(List.of(firstStep, secondStep));
        path = paths.save(path);

        Question pending = generatedExercise(addition, "Tính 36 + 27 = ?", "63", ApprovalStatus.PENDING);
        Question approved = generatedExercise(addition, "Tính 48 + 16 = ?", "64", ApprovalStatus.APPROVED);
        exercises.save(linkExercise(firstStep, pending));
        exercises.save(linkExercise(firstStep, approved));

        log.info("Seeded synthetic demo data: teacher={}, students=2, class={}, assessment={}, path={}, masteryRecords={}",
                TEACHER_EMAIL, classroom.getName(), assessment.getTitle(), path.getId(),
                mastery.findByStudentClassroomId(classroom.getId()).size());
    }

    private void setAccount(User user, String name, String email) {
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
    }

    private MultipleChoiceQuestion multipleChoice(Lesson lesson, String content, String correct,
            String wrong1, String wrong2) {
        MultipleChoiceQuestion question = new MultipleChoiceQuestion();
        question.setContent(content);
        question.setDifficulty(Difficulty.MEDIUM);
        question.setScore(5.0);
        question.setLesson(lesson);
        question.setAiGenerated(false);
        question.setApprovalStatus(ApprovalStatus.APPROVED);
        question.setExplanation("Cộng hàng đơn vị trước rồi cộng hàng chục, nhớ thêm một chục nếu cần.");
        question.setAllowMultiple(false);
        Option correctOption = option(correct, true, 0);
        question.setOptions(List.of(correctOption, option(wrong1, false, 1), option(wrong2, false, 2)));
        return questions.save(question);
    }

    private Option option(String name, boolean correct, int order) {
        Option option = new Option();
        option.setName(name);
        option.setCorrect(correct);
        option.setDisplayOrder(order);
        return option;
    }

    private ShortAnswerQuestion shortAnswer(Lesson lesson, String content, String correctAnswer,
            boolean aiGenerated) {
        ShortAnswerQuestion question = new ShortAnswerQuestion();
        question.setContent(content);
        question.setDifficulty(Difficulty.MEDIUM);
        question.setScore(5.0);
        question.setLesson(lesson);
        question.setAiGenerated(aiGenerated);
        question.setApprovalStatus(ApprovalStatus.APPROVED);
        question.setExplanation("Thực hiện phép tính theo từng hàng rồi kiểm tra lại kết quả.");
        question.setCaseSensitive(false);
        question.setCorrectAnswer(correctAnswer);
        return questions.save(question);
    }

    private StudentAnswer answer(Question question, boolean correct, double score, String content, Option selected) {
        if (question instanceof MultipleChoiceQuestion) {
            MultipleChoiceAnswer answer = new MultipleChoiceAnswer();
            answer.setQuestion(question);
            answer.setCorrect(correct);
            answer.setAwardedScore(score);
            if (selected != null) answer.setSelectedOptions(Set.of(selected));
            return answer;
        }
        ShortAnswerAnswer answer = new ShortAnswerAnswer();
        answer.setQuestion(question);
        answer.setCorrect(correct);
        answer.setAwardedScore(score);
        answer.setContent(content);
        return answer;
    }

    private KnowledgeMastery addMastery(Student student, Lesson lesson, double score,
            MasteryLevel level, double confidence) {
        KnowledgeMastery value = new KnowledgeMastery();
        value.setStudent(student);
        value.setLesson(lesson);
        value.setMasteryScore(score);
        value.setMasteryLevel(level);
        value.setConfidenceScore(confidence);
        value.setLastUpdated(Instant.now());
        return mastery.save(value);
    }

    private void addGap(Student student, Lesson lesson, GapPriority priority, String cause, GapStatus status) {
        KnowledgeGap gap = new KnowledgeGap();
        gap.setStudent(student);
        gap.setLesson(lesson);
        gap.setPriority(priority);
        gap.setRootCause(cause);
        gap.setStatus(status);
        gaps.save(gap);
    }

    private LearningStep step(LearningPath path, int order, LearningStepStatus status, Lesson lesson) {
        LearningStep step = new LearningStep();
        step.setPath(path);
        step.setStepOrder(order);
        step.setStatus(status);
        step.setTargetScore(70.0);
        step.setLesson(lesson);
        return step;
    }

    private Question generatedExercise(Lesson lesson, String content, String answer, ApprovalStatus status) {
        ShortAnswerQuestion question = shortAnswer(lesson, content, answer, true);
        question.setApprovalStatus(status);
        return questions.save(question);
    }

    private StepExercise linkExercise(LearningStep step, Question question) {
        StepExercise link = new StepExercise();
        link.setStep(step);
        link.setQuestion(question);
        return link;
    }
}
