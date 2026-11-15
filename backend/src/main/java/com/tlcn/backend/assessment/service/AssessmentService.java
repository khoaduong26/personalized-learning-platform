package com.tlcn.backend.assessment.service;

import com.tlcn.backend.assessment.entity.*;
import java.util.List;

public interface AssessmentService {
    List<Assessment> list();

    Assessment get(Long id);

    List<Assessment> byTeacher(Long id);

    Assessment save(Assessment value);

    List<Question> questionsForLesson(Long lessonId);

    List<AssessmentAttempt> attemptsForStudent(Long id);

}
