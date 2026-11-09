package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
@Entity @Table(name = "short_answer_answers")
public class ShortAnswerAnswer extends StudentAnswer {
    @Column(columnDefinition = "TEXT") private String content;
    protected ShortAnswerAnswer() {}
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
