package com.tlcn.backend.learningpath.generation;

import java.util.List;

public interface LearningPathOrderReviewer {
    ReviewResult review(List<LearningPathOrderer.Candidate> candidates, List<Integer> orderedPositions);

    record ReviewResult(boolean accepted, String feedback) {
    }
}
