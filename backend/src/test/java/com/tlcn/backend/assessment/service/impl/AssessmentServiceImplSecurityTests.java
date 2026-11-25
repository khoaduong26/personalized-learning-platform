package com.tlcn.backend.assessment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tlcn.backend.assessment.dto.AssessmentRequest;
import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.AssessmentType;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.repository.AssessmentAttemptRepository;
import com.tlcn.backend.assessment.repository.AssessmentRepository;
import com.tlcn.backend.assessment.repository.FillBlankQuestionRepository;
import com.tlcn.backend.assessment.repository.MultipleChoiceQuestionRepository;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.assessment.repository.ShortAnswerQuestionRepository;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.repository.StudentRepository;
import com.tlcn.backend.user.repository.TeacherRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AssessmentServiceImplSecurityTests {
    private static final UserSummary TEACHER_ACTOR =
            new UserSummary(7L, "Teacher", "teacher@example.invalid", Role.TEACHER);

    private final AssessmentRepository assessments = mock(AssessmentRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
    private final LessonRepository lessons = mock(LessonRepository.class);
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final MultipleChoiceQuestionRepository multipleChoice = mock(MultipleChoiceQuestionRepository.class);
    private final FillBlankQuestionRepository fillBlank = mock(FillBlankQuestionRepository.class);
    private final ShortAnswerQuestionRepository shortAnswer = mock(ShortAnswerQuestionRepository.class);
    private final ClassroomRepository classrooms = mock(ClassroomRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);

    @Test
    void teacherCannotAssignPendingAiGeneratedQuestionToAssessment() {
        Question pending = mock(Question.class);
        Teacher teacher = mock(Teacher.class);
        when(pending.isAiGenerated()).thenReturn(true);
        when(pending.getApprovalStatus()).thenReturn(ApprovalStatus.PENDING);
        when(teachers.findById(TEACHER_ACTOR.id())).thenReturn(Optional.of(teacher));
        when(questions.findAllById(List.of(19L))).thenReturn(List.of(pending));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().save(new AssessmentRequest("Ôn tập", AssessmentType.PRACTICE, 20,
                        List.of(19L), List.of(23L)), TEACHER_ACTOR));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(assessments, never()).save(any());
    }

    @Test
    void teacherCannotReadAttemptHistoryForStudentOutsideTheirClass() {
        Student student = mock(Student.class);
        Classroom classroom = mock(Classroom.class);
        Teacher owner = mock(Teacher.class);
        when(attempts.findByStudentIdOrderByStartedAtDesc(42L)).thenReturn(List.of());
        when(students.findById(42L)).thenReturn(Optional.of(student));
        when(student.getClassroom()).thenReturn(classroom);
        when(classroom.getTeacher()).thenReturn(owner);
        when(owner.getId()).thenReturn(88L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().attemptsForStudent(42L, TEACHER_ACTOR));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private AssessmentServiceImpl service() {
        return new AssessmentServiceImpl(assessments, questions, attempts, lessons, teachers,
                multipleChoice, fillBlank, shortAnswer, classrooms, students);
    }
}
