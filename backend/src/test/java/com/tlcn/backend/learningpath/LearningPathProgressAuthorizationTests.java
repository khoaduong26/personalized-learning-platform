package com.tlcn.backend.learningpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.generation.LearningPathOrderer;
import com.tlcn.backend.learningpath.repository.LearningPathRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.learningpath.service.impl.LearningPathServiceImpl;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LearningPathProgressAuthorizationTests {
    private final LearningPathRepository paths = mock(LearningPathRepository.class);
    private final LearningStepRepository steps = mock(LearningStepRepository.class);
    private final KnowledgeGapRepository gaps = mock(KnowledgeGapRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final LearningPathOrderer orderer = mock(LearningPathOrderer.class);

    @Test
    void studentCannotMarkStepCompleteWithoutPassingPracticeSubmission() {
        Student student = mock(Student.class);
        LearningPath path = mock(LearningPath.class);
        LearningStep step = mock(LearningStep.class);
        UserSummary actor = new UserSummary(7L, "Student", "student@example.invalid", Role.STUDENT);
        when(students.findById(7L)).thenReturn(Optional.of(student));
        when(paths.findByIdAndStudentId(20L, 7L)).thenReturn(Optional.of(path));
        when(path.getStatus()).thenReturn(LearningPathStatus.ACTIVE);
        when(steps.findByIdAndPathId(30L, 20L)).thenReturn(Optional.of(step));
        when(step.getStatus()).thenReturn(LearningStepStatus.IN_PROGRESS);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().updateStepStatus(7L, 20L, 30L, LearningStepStatus.COMPLETED, actor));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(steps, never()).save(step);
        verify(paths, never()).save(path);
    }

    private LearningPathServiceImpl service() {
        return new LearningPathServiceImpl(paths, steps, gaps, students, orderer);
    }
}
