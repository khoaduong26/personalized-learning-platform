package com.tlcn.backend.assessment.service;

import com.tlcn.backend.assessment.entity.*;
import java.util.List;
import com.tlcn.backend.assessment.dto.AssessmentRequest;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.classroom.entity.Classroom;

public interface AssessmentService {
    List<Assessment> list(UserSummary actor);

    Assessment get(Long id, UserSummary actor);

    Assessment save(AssessmentRequest value, UserSummary actor);

    Assessment publish(Long id, UserSummary actor);

    List<Question> questionsForLesson(Long lessonId, UserSummary actor);

    Question createQuestion(QuestionRequest request, UserSummary actor);

    List<AssessmentAttempt> attemptsForStudent(Long id, Long classroomId, UserSummary actor);

    default List<AssessmentAttempt> attemptsForStudent(Long id, UserSummary actor) {
        return attemptsForStudent(id, null, actor);
    }

    List<Classroom> classroomsForTeacher(UserSummary actor);

    Assessment ownedAssessment(Long id, UserSummary actor);

}
