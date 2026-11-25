package com.tlcn.backend.mastery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Assessment;
import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import com.tlcn.backend.assessment.repository.StudentAnswerRepository;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.mastery.entity.GapPriority;
import com.tlcn.backend.mastery.entity.GapStatus;
import com.tlcn.backend.mastery.entity.KnowledgeGap;
import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import com.tlcn.backend.mastery.entity.MasteryHistory;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.MasteryHistoryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.mastery.service.impl.MasteryServiceImpl;
import com.tlcn.backend.user.entity.Student;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MasteryLearningLoopTests {
    private final KnowledgeMasteryRepository mastery = mock(KnowledgeMasteryRepository.class);
    private final MasteryHistoryRepository history = mock(MasteryHistoryRepository.class);
    private final StudentErrorRepository errors = mock(StudentErrorRepository.class);
    private final KnowledgeGapRepository gaps = mock(KnowledgeGapRepository.class);
    private final StudentAnswerRepository answers = mock(StudentAnswerRepository.class);
    private final ClassroomRepository classrooms = mock(ClassroomRepository.class);
    private final LearningStepRepository learningSteps = mock(LearningStepRepository.class);

    @Test
    void passingPracticeCompletesStepOpensNextAndRecordsMasteryHistory() {
        Fixture f = new Fixture(true, 75.0);
        when(mastery.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(gaps.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(learningSteps.findByPathIdOrderByStepOrderAsc(3L)).thenReturn(List.of(f.step, f.nextStep));

        service().analyze(f.attempt);

        assertEquals(LearningStepStatus.COMPLETED, f.step.getStatus());
        assertEquals(LearningStepStatus.IN_PROGRESS, f.nextStep.getStatus());
        assertEquals(LearningPathStatus.ACTIVE, f.path.getStatus());
        verify(mastery).save(any(KnowledgeMastery.class));
        verify(history).save(any(MasteryHistory.class));
    }

    @Test
    void failingPracticeKeepsStepActiveAndCreatesRemediationGap() {
        Fixture f = new Fixture(false, 70.0);
        when(mastery.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(gaps.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(learningSteps.findByPathIdOrderByStepOrderAsc(3L)).thenReturn(List.of(f.step, f.nextStep));

        service().analyze(f.attempt);

        assertEquals(LearningStepStatus.IN_PROGRESS, f.step.getStatus());
        assertEquals(LearningStepStatus.LOCKED, f.nextStep.getStatus());
        assertEquals(LearningPathStatus.ACTIVE, f.path.getStatus());
        var gap = org.mockito.ArgumentCaptor.forClass(KnowledgeGap.class);
        verify(gaps).save(gap.capture());
        assertEquals(GapStatus.IN_REMEDIATION, gap.getValue().getStatus());
        assertEquals(GapPriority.HIGH, gap.getValue().getPriority());
    }

    @Test
    void passingLastStepCompletesTheLearningPath() {
        Fixture f = new Fixture(true, 70.0);
        when(mastery.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(gaps.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.empty());
        when(learningSteps.findByPathIdOrderByStepOrderAsc(3L)).thenReturn(List.of(f.step));

        service().analyze(f.attempt);

        verify(f.path).setStatus(LearningPathStatus.COMPLETED);
    }

    @Test
    void passingAssessmentResolvesExistingGap() {
        Fixture f = new Fixture(true, null);
        KnowledgeMastery existing = mock(KnowledgeMastery.class);
        when(existing.getId()).thenReturn(31L);
        when(existing.getMasteryScore()).thenReturn(80.0);
        when(mastery.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.of(existing));
        when(history.existsByMasteryIdAndTriggerSource(31L, "ASSESSMENT_101")).thenReturn(false);
        when(history.countByMasteryId(31L)).thenReturn(1L);
        KnowledgeGap existingGap = new KnowledgeGap();
        existingGap.setStatus(GapStatus.IN_REMEDIATION);
        when(gaps.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.of(existingGap));

        service().analyze(f.asAssessmentAttempt(101L));

        assertEquals(GapStatus.RESOLVED, existingGap.getStatus());
        verify(existing).setConfidenceScore(0.4);
        var change = org.mockito.ArgumentCaptor.forClass(MasteryHistory.class);
        verify(history).save(change.capture());
        assertEquals("ASSESSMENT_101", change.getValue().getTriggerSource());
        assertEquals(90.0, change.getValue().getNewScore());
        verify(errors, never()).save(any());
    }

    @Test
    void repeatedAssessmentAnalysisDoesNotDuplicateMasteryHistoryOrProgress() {
        Fixture f = new Fixture(true, null);
        KnowledgeMastery existing = mock(KnowledgeMastery.class);
        when(existing.getId()).thenReturn(31L);
        when(mastery.findByStudentIdAndLessonId(7L, 21L)).thenReturn(Optional.of(existing));
        when(history.existsByMasteryIdAndTriggerSource(31L, "ASSESSMENT_101")).thenReturn(true);

        service().analyze(f.asAssessmentAttempt(101L));

        verify(mastery, never()).save(any());
        verify(history, never()).save(any());
        verify(gaps, never()).save(any());
        verify(learningSteps, never()).save(any());
    }

    private MasteryServiceImpl service() {
        return new MasteryServiceImpl(mastery, history, errors, gaps, answers, mock(com.tlcn.backend.user.repository.StudentRepository.class), classrooms, learningSteps);
    }

    private final class Fixture {
        private final Lesson lesson = mock(Lesson.class);
        private final Question question = mock(Question.class);
        private final Student student = mock(Student.class);
        private final AssessmentAttempt attempt = mock(AssessmentAttempt.class);
        private final LearningPath path = mock(LearningPath.class);
        private final LearningStep step = new LearningStep();
        private final LearningStep nextStep = new LearningStep();
        private final StudentAnswer answer = mock(StudentAnswer.class);

        private Fixture(boolean correct, Double targetScore) {
            when(lesson.getId()).thenReturn(21L);
            when(question.getId()).thenReturn(41L);
            when(question.getLesson()).thenReturn(lesson);
            when(question.getApprovalStatus()).thenReturn(ApprovalStatus.APPROVED);
            when(student.getId()).thenReturn(7L);
            when(attempt.getId()).thenReturn(101L);
            when(attempt.getStudent()).thenReturn(student);
            when(answer.getId()).thenReturn(51L);
            when(answer.getQuestion()).thenReturn(question);
            when(answer.isCorrect()).thenReturn(correct);
            when(answers.findByAttemptId(101L)).thenReturn(List.of(answer));
            when(errors.existsByAnswerId(51L)).thenReturn(false);
            when(history.countByMasteryId(any())).thenReturn(0L);

            step.setStepOrder(1);
            step.setStatus(LearningStepStatus.IN_PROGRESS);
            step.setTargetScore(targetScore);
            step.setLesson(lesson);
            step.setPath(path);
            nextStep.setStepOrder(2);
            nextStep.setStatus(LearningStepStatus.LOCKED);
            when(path.getId()).thenReturn(3L);
            when(path.getVersion()).thenReturn(1);
            when(path.getStatus()).thenReturn(LearningPathStatus.ACTIVE);
            when(attempt.getLearningStep()).thenReturn(step);
            StepExercise exercise = new StepExercise();
            exercise.setStep(step);
            exercise.setQuestion(question);
            step.getExercises().add(exercise);
        }

        private AssessmentAttempt asAssessmentAttempt(Long attemptId) {
            Assessment assessment = new Assessment();
            assessment.setQuestions(List.of(question));
            when(attempt.getId()).thenReturn(attemptId);
            when(attempt.getAssessment()).thenReturn(assessment);
            when(attempt.getLearningStep()).thenReturn(null);
            when(answers.findByAttemptId(attemptId)).thenReturn(List.of(answer));
            when(errors.existsByAnswerId(51L)).thenReturn(false);
            return attempt;
        }
    }
}
