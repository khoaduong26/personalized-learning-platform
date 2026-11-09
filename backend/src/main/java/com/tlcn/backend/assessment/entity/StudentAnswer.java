package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "student_answers") @Inheritance(strategy = InheritanceType.JOINED)
public abstract class StudentAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private boolean correct;
    @Column(columnDefinition = "TEXT") private String teacherComment;
    private Instant submittedAt = Instant.now();
    private Double awardedScore;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private Question question;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "attempt_id") private AssessmentAttempt attempt;
    protected StudentAnswer() {}
    public Long getId() { return id; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean value) { correct = value; }
    public String getTeacherComment() { return teacherComment; }
    public void setTeacherComment(String value) { teacherComment = value; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Double getAwardedScore() { return awardedScore; }
    public void setAwardedScore(Double value) { awardedScore = value; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question value) { question = value; }
    public AssessmentAttempt getAttempt() { return attempt; }
    public void setAttempt(AssessmentAttempt value) { attempt = value; }
    public double score() { return awardedScore == null ? 0.0 : awardedScore; }
}
