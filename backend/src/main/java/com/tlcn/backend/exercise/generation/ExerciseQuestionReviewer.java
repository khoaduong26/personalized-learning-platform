package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.learningpath.entity.LearningStep;

public interface ExerciseQuestionReviewer {
    ReviewResult review(LearningStep step, QuestionRequest question);

    record ReviewResult(boolean accepted, String feedback) {
    }
}
