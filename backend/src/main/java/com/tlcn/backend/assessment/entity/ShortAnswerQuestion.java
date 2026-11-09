package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
@Entity @Table(name = "short_answer_questions")
public class ShortAnswerQuestion extends Question {
    @Column(nullable = false) private boolean caseSensitive;
    @Column(nullable = false, columnDefinition = "TEXT") private String correctAnswer;
    protected ShortAnswerQuestion() {}
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean value) { caseSensitive = value; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String value) { correctAnswer = value; }
}
