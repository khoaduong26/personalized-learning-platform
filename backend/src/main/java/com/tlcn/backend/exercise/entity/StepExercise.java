package com.tlcn.backend.exercise.entity;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.learningpath.entity.LearningStep;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "step_exercises", uniqueConstraints = @UniqueConstraint(columnNames = {"learning_step_id", "question_id"}))
public class StepExercise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "learning_step_id") private LearningStep step;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id", unique = true) private Question question;
    @Column(nullable = false) private Instant generatedAt = Instant.now();
    protected StepExercise() {}
    public Long getId() { return id; }
    public LearningStep getStep() { return step; }
    public void setStep(LearningStep value) { step = value; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question value) { question = value; }
    public Instant getGeneratedAt() { return generatedAt; }
}
