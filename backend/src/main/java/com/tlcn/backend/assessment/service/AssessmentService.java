package com.tlcn.backend.assessment.service;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class AssessmentService {
    private final AssessmentRepository assessments; private final QuestionRepository questions; private final AssessmentAttemptRepository attempts;
    public AssessmentService(AssessmentRepository assessments, QuestionRepository questions, AssessmentAttemptRepository attempts) { this.assessments=assessments; this.questions=questions; this.attempts=attempts; }
    @Transactional(readOnly=true) public List<Assessment> list() { return assessments.findAll(); }
    @Transactional(readOnly=true) public Assessment get(Long id) { return assessments.findById(id).orElseThrow(() -> new IllegalArgumentException("Assessment not found")); }
    @Transactional(readOnly=true) public List<Assessment> byTeacher(Long id) { return assessments.findByTeacherId(id); }
    @Transactional public Assessment save(Assessment value) { return assessments.save(value); }
    @Transactional(readOnly=true) public List<Question> questionsForLesson(Long lessonId) { return questions.findByLessonId(lessonId); }
    @Transactional(readOnly=true) public List<AssessmentAttempt> attemptsForStudent(Long id) { return attempts.findByStudentIdOrderByStartedAtDesc(id); }
}
