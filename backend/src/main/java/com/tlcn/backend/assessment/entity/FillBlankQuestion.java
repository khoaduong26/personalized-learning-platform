package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "fill_blank_questions")
public class FillBlankQuestion extends Question {
    @Column(nullable = false) private boolean caseSensitive;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("position ASC") private List<Blank> blanks = new ArrayList<>();
    protected FillBlankQuestion() {}
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean value) { caseSensitive = value; }
    public List<Blank> getBlanks() { return blanks; }
}
