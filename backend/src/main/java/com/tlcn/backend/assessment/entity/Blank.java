package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
@Entity @Table(name = "question_blanks")
public class Blank {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, columnDefinition = "TEXT") private String correctAnswer;
    @Column(nullable = false) private Integer position;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private FillBlankQuestion question;
    protected Blank() {}
    public Long getId() { return id; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String value) { correctAnswer = value; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public FillBlankQuestion getQuestion() { return question; }
    public void setQuestion(FillBlankQuestion question) { this.question = question; }
}
