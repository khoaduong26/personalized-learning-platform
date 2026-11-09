package com.tlcn.backend.mastery.entity;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.user.entity.Student;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "knowledge_mastery", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "lesson_id"}))
public class KnowledgeMastery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lesson_id") private Lesson lesson;
    @Column(nullable = false) private Double masteryScore = 0.0;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MasteryLevel masteryLevel = MasteryLevel.NOT_STARTED;
    @Column(nullable = false) private Double confidenceScore = 0.0;
    @Column(nullable = false) private Instant lastUpdated = Instant.now();
    protected KnowledgeMastery() {}
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson value) { lesson = value; }
    public Double getMasteryScore() { return masteryScore; }
    public void setMasteryScore(Double value) { masteryScore = value; }
    public MasteryLevel getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(MasteryLevel value) { masteryLevel = value; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double value) { confidenceScore = value; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant value) { lastUpdated = value; }
}
