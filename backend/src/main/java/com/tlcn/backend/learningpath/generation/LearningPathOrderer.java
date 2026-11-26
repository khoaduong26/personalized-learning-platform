package com.tlcn.backend.learningpath.generation;

import java.util.List;

public interface LearningPathOrderer {
    List<Integer> order(List<Candidate> candidates);

    record Candidate(int position, String lessonName, String lessonDescription, String priority) {
    }
}
