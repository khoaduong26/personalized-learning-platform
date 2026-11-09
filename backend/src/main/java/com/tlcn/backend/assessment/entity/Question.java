package com.tlcn.backend.assessment.entity;

import com.tlcn.backend.academic.entity.Lesson;
import jakarta.persistence.*;

@Entity @Table(name = "questions") @Inheritance(strategy = InheritanceType.JOINED)
public abstract class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Difficulty difficulty;
    @Column(nullable = false) private Double score;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lesson_id") private Lesson lesson;
    @Column(nullable = false) private boolean aiGenerated;
    @Enumerated(EnumType.STRING) @Column(length = 20) private ApprovalStatus approvalStatus;
    @Column(columnDefinition = "TEXT") private String explanation;
    protected Question() {}
    public Long getId() { return id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
