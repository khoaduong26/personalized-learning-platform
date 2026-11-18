package com.tlcn.backend.assessment.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multiple_choice_questions")
public class MultipleChoiceQuestion extends Question {
    @Column(nullable = false)
    private boolean allowMultiple;
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Option> options = new ArrayList<>();

    public MultipleChoiceQuestion() {
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public void setAllowMultiple(boolean value) {
        allowMultiple = value;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> values) {
        options.clear();
        values.forEach(value -> {
            value.setQuestion(this);
            options.add(value);
        });
    }
}
