package com.tlcn.backend.exercise.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Difficulty;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.exercise.generation.ExerciseQuestionGenerator;
import com.tlcn.backend.exercise.generation.RuleBasedExerciseQuestionGenerator;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ExerciseServiceImplTests {
    private static final Long STEP_ID = 34L;
    private static final Long LESSON_ID = 12L;
    private static final UserSummary ADMIN = new UserSummary(1L, "Admin", "admin@example.invalid", Role.ADMIN);
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final StepExerciseRepository exercises = mock(StepExerciseRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final LearningStepRepository steps = mock(LearningStepRepository.class);
    private final LessonRepository lessons = mock(LessonRepository.class);
    private final LearningStep step = mock(LearningStep.class);
    private final Lesson lesson = mock(Lesson.class);

    @BeforeEach
    void setUp() {
        when(step.getLesson()).thenReturn(lesson);
        when(lesson.getId()).thenReturn(LESSON_ID);
        when(lesson.getName()).thenReturn("Phép cộng trong phạm vi 10");
        when(steps.findById(STEP_ID)).thenReturn(Optional.of(step));
        when(lessons.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
        when(questions.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(exercises.save(any(StepExercise.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void demoGeneratorCreatesPendingQuestionLinkedToSelectedStepLesson() {
        ExerciseServiceImpl service = service(new RuleBasedExerciseQuestionGenerator());

        var result = service.generateForStep(STEP_ID, ADMIN);

        assertEquals("demo-rule-based", result.provider());
        assertTrue(result.demo());
        assertTrue(result.notice().contains("chưa kết nối AI bên ngoài"));
        assertTrue(result.question().getContent().startsWith("[DEMO]"));
        assertEquals(LESSON_ID, result.question().getLesson().getId());
        assertTrue(result.question().isAiGenerated());
        assertEquals(ApprovalStatus.PENDING, result.question().getApprovalStatus());

        var exerciseCaptor = org.mockito.ArgumentCaptor.forClass(StepExercise.class);
        verify(exercises).save(exerciseCaptor.capture());
        assertSame(step, exerciseCaptor.getValue().getStep());
        assertSame(result.question(), exerciseCaptor.getValue().getQuestion());
    }

    @Test
    void generatorOutputForDifferentLessonIsRejectedBeforeSaving() {
        QuestionRequest mismatchedQuestion = new QuestionRequest("2 + 3 = ?", Difficulty.EASY, 1.0, 99L,
                "MULTIPLE_CHOICE", false, null, null,
                java.util.List.of(new QuestionRequest.OptionInput("4", false, 0),
                        new QuestionRequest.OptionInput("5", true, 1)),
                null, null);
        ExerciseQuestionGenerator invalidGenerator = ignored -> new ExerciseQuestionGenerator.GeneratedExercise(
                mismatchedQuestion, "test-adapter", false, "test");
        ExerciseServiceImpl service = service(invalidGenerator);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.generateForStep(STEP_ID, ADMIN));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        verify(questions, never()).save(any(Question.class));
        verify(exercises, never()).save(any(StepExercise.class));
    }

    @Test
    void invalidGeneratorPayloadIsRejectedAsIntegrationFailure() {
        QuestionRequest invalidQuestion = new QuestionRequest(" ", null, 0.0, LESSON_ID,
                "MULTIPLE_CHOICE", false, null, null, java.util.List.of(), null, null);
        ExerciseQuestionGenerator invalidGenerator = ignored -> new ExerciseQuestionGenerator.GeneratedExercise(
                invalidQuestion, "test-adapter", false, "test");
        ExerciseServiceImpl service = service(invalidGenerator);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.generateForStep(STEP_ID, ADMIN));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertFalse(exception.getReason().isBlank());
        verify(questions, never()).save(any(Question.class));
    }

    @Test
    void generatorFailureDoesNotPersistPartialExercise() {
        ExerciseQuestionGenerator failingGenerator = ignored -> {
            throw new IllegalStateException("provider unavailable");
        };
        ExerciseServiceImpl service = service(failingGenerator);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.generateForStep(STEP_ID, ADMIN));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        verify(questions, never()).save(any(Question.class));
        verify(exercises, never()).save(any(StepExercise.class));
    }

    @Test
    void studentCannotViewExercisesForLockedStep() {
        Student student = mock(Student.class);
        LearningPath path = mock(LearningPath.class);
        when(student.getId()).thenReturn(7L);
        when(path.getStudent()).thenReturn(student);
        when(step.getPath()).thenReturn(path);
        when(step.getStatus()).thenReturn(LearningStepStatus.LOCKED);
        UserSummary learner = new UserSummary(7L, "Student", "student@example.invalid", Role.STUDENT);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service(new RuleBasedExerciseQuestionGenerator()).forStep(STEP_ID, learner));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(exercises, never()).findByStepId(STEP_ID);
    }

    private ExerciseServiceImpl service(ExerciseQuestionGenerator generator) {
        return new ExerciseServiceImpl(exercises, questions, steps, lessons, generator, VALIDATOR);
    }
}
