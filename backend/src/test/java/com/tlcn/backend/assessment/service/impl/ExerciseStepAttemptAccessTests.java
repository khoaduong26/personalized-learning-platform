package com.tlcn.backend.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.assessment.repository.AssessmentAttemptRepository;
import com.tlcn.backend.assessment.repository.FillBlankAnswerRepository;
import com.tlcn.backend.assessment.repository.MultipleChoiceAnswerRepository;
import com.tlcn.backend.assessment.repository.ShortAnswerAnswerRepository;
import com.tlcn.backend.assessment.repository.StudentAnswerRepository;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ExerciseStepAttemptAccessTests {
    private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
    private final StudentAnswerRepository answers = mock(StudentAnswerRepository.class);
    private final MultipleChoiceAnswerRepository multipleAnswers = mock(MultipleChoiceAnswerRepository.class);
    private final FillBlankAnswerRepository fillAnswers = mock(FillBlankAnswerRepository.class);
    private final ShortAnswerAnswerRepository shortAnswers = mock(ShortAnswerAnswerRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final AssessmentService assessments = mock(AssessmentService.class);
    private final MasteryService mastery = mock(MasteryService.class);
    private final LearningStepRepository steps = mock(LearningStepRepository.class);
    private final StepExerciseRepository exercises = mock(StepExerciseRepository.class);

    @Test
    void studentCannotStartPracticeOnLockedStep() {
        Student student = mock(Student.class);
        LearningPath path = mock(LearningPath.class);
        LearningStep step = mock(LearningStep.class);
        when(steps.findById(10L)).thenReturn(Optional.of(step));
        when(step.getPath()).thenReturn(path);
        when(path.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(7L);
        when(step.getStatus()).thenReturn(LearningStepStatus.LOCKED);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().startExercise(10L,
                        new UserSummary(7L, "Student", "student@example.invalid", Role.STUDENT)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(exercises, never()).findByStepId(10L);
        verify(attempts, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private AttemptServiceImpl service() {
        return new AttemptServiceImpl(attempts, answers, multipleAnswers, fillAnswers, shortAnswers,
                students, assessments, mastery, steps, exercises);
    }
}
