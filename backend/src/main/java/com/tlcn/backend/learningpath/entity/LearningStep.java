package com.tlcn.backend.learningpath.entity;
import com.tlcn.backend.academic.entity.Lesson;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "learning_steps", uniqueConstraints = @UniqueConstraint(columnNames = {"path_id", "step_order"}))
public class LearningStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "step_order", nullable = false) private Integer stepOrder;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LearningStepStatus status = LearningStepStatus.LOCKED;
    private Double targetScore;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "path_id") private LearningPath path;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lesson_id") private Lesson lesson;
    @OneToMany(mappedBy = "step") private List<com.tlcn.backend.exercise.entity.StepExercise> exercises = new ArrayList<>();
    protected LearningStep() {}
    public Long getId() { return id; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer value) { stepOrder = value; }
    public LearningStepStatus getStatus() { return status; }
    public void setStatus(LearningStepStatus value) { status = value; }
    public Double getTargetScore() { return targetScore; }
    public void setTargetScore(Double value) { targetScore = value; }
    public LearningPath getPath() { return path; }
    public void setPath(LearningPath value) { path = value; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson value) { lesson = value; }
    public List<com.tlcn.backend.exercise.entity.StepExercise> getExercises() { return exercises; }
}
