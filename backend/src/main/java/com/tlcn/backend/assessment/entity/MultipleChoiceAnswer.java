package com.tlcn.backend.assessment.entity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
@Entity @Table(name = "multiple_choice_answers")
public class MultipleChoiceAnswer extends StudentAnswer {
    @ManyToMany @JoinTable(name = "answer_selected_options", joinColumns = @JoinColumn(name = "answer_id"), inverseJoinColumns = @JoinColumn(name = "option_id")) private Set<Option> selectedOptions = new HashSet<>();
    protected MultipleChoiceAnswer() {}
    public Set<Option> getSelectedOptions() { return selectedOptions; }
}
