package com.tlcn.backend.assessment.service.impl;

import com.tlcn.backend.assessment.entity.Assessment;
import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.repository.AssessmentAttemptRepository;
import com.tlcn.backend.assessment.repository.AssessmentRepository;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.assessment.service.AssessmentService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentServiceImpl implements AssessmentService {
    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AssessmentAttemptRepository attempts;

    public AssessmentServiceImpl(AssessmentRepository assessments, QuestionRepository questions,
            AssessmentAttemptRepository attempts) {
        this.assessments = assessments;
        this.questions = questions;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public List<Assessment> list() {
        return assessments.findAll();
    }

    @Transactional(readOnly = true)
    public Assessment get(Long id) {
        return assessments.findById(id).orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
    }

    @Transactional(readOnly = true)
    public List<Assessment> byTeacher(Long id) {
        return assessments.findByTeacherId(id);
    }

    @Transactional
    public Assessment save(Assessment value) {
        return assessments.save(value);
    }

    @Transactional(readOnly = true)
    public List<Question> questionsForLesson(Long lessonId) {
        return questions.findByLessonId(lessonId);
    }

    @Transactional(readOnly = true)
    public List<AssessmentAttempt> attemptsForStudent(Long id) {
        return attempts.findByStudentIdOrderByStartedAtDesc(id);
    }
}
