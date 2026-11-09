package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
@Entity @Table(name = "student_blank_answers")
public class StudentBlankAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "blank_id") private Blank blank;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "answer_id") private FillBlankAnswer answer;
    protected StudentBlankAnswer() {}
    public Long getId() { return id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Blank getBlank() { return blank; }
    public void setBlank(Blank value) { blank = value; }
    public FillBlankAnswer getAnswer() { return answer; }
    public void setAnswer(FillBlankAnswer value) { answer = value; }
}
