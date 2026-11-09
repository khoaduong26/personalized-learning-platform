package com.tlcn.backend.assessment.entity;
import com.tlcn.backend.user.entity.Student;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "assessment_attempts")
public class AssessmentAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant startedAt = Instant.now();
    private Instant submittedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AttemptStatus status = AttemptStatus.IN_PROGRESS;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assessment_id") private Assessment assessment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "learning_step_id") private com.tlcn.backend.learningpath.entity.LearningStep learningStep;
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true) private List<StudentAnswer> answers = new ArrayList<>();
    protected AssessmentAttempt() {}
    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant value) { submittedAt = value; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus value) { status = value; }
    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment value) { assessment = value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public com.tlcn.backend.learningpath.entity.LearningStep getLearningStep() { return learningStep; }
    public void setLearningStep(com.tlcn.backend.learningpath.entity.LearningStep value) { learningStep = value; }
    public List<StudentAnswer> getAnswers() { return answers; }
    public double totalScore() { return answers.stream().mapToDouble(a -> a.getAwardedScore() == null ? 0 : a.getAwardedScore()).sum(); }
}
