package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
@Entity @Table(name = "question_options")
public class Option {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, columnDefinition = "TEXT") private String name;
    @Column(nullable = false) private boolean correct;
    @Column(nullable = false) private Integer displayOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private MultipleChoiceQuestion question;
    protected Option() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public MultipleChoiceQuestion getQuestion() { return question; }
    public void setQuestion(MultipleChoiceQuestion question) { this.question = question; }
}
