package com.tlcn.backend.mastery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.assessment.repository.StudentAnswerRepository;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.MasteryHistoryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.mastery.service.impl.MasteryServiceImpl;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MasteryServiceAuthorizationTests {
    private final KnowledgeMasteryRepository mastery = mock(KnowledgeMasteryRepository.class);
    private final MasteryHistoryRepository history = mock(MasteryHistoryRepository.class);
    private final StudentErrorRepository errors = mock(StudentErrorRepository.class);
    private final KnowledgeGapRepository gaps = mock(KnowledgeGapRepository.class);
    private final StudentAnswerRepository answers = mock(StudentAnswerRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final ClassroomRepository classrooms = mock(ClassroomRepository.class);
    private final LearningStepRepository learningSteps = mock(LearningStepRepository.class);

    @Test
    void teacherCannotReadDashboardForAnotherTeachersClass() {
        Classroom classroom = mock(Classroom.class);
        Teacher owner = mock(Teacher.class);
        when(classrooms.findById(31L)).thenReturn(Optional.of(classroom));
        when(classroom.getTeacher()).thenReturn(owner);
        when(owner.getId()).thenReturn(44L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().classroomDashboard(31L,
                        new UserSummary(12L, "Teacher", "teacher@example.invalid", Role.TEACHER)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(mastery, never()).findByStudentClassroomId(31L);
    }

    private MasteryServiceImpl service() {
        return new MasteryServiceImpl(mastery, history, errors, gaps, answers, students, classrooms, learningSteps);
    }
}
