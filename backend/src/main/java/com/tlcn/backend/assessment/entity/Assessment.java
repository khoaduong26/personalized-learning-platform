package com.tlcn.backend.assessment.entity;

import com.tlcn.backend.user.entity.Teacher;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "assessments")
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 180) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AssessmentType type;
    private Integer durationMinutes;
    @Column(nullable = false) private boolean published;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id") private Teacher teacher;
    @ManyToMany @JoinTable(name = "assessment_questions", joinColumns = @JoinColumn(name = "assessment_id"), inverseJoinColumns = @JoinColumn(name = "question_id")) @OrderColumn(name = "display_order") private List<Question> questions = new ArrayList<>();
    protected Assessment() {}
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AssessmentType getType() { return type; }
    public void setType(AssessmentType type) { this.type = type; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getCreatedAt() { return createdAt; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public List<Question> getQuestions() { return questions; }
}
