package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "fill_blank_answers")
public class FillBlankAnswer extends StudentAnswer {
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true) private List<StudentBlankAnswer> answers = new ArrayList<>();
    protected FillBlankAnswer() {}
    public List<StudentBlankAnswer> getAnswers() { return answers; }
}
