package com.tlcn.backend.exercise.generation;

import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.learningpath.entity.LearningStep;

/** Adapter boundary for exercise generators. External providers can implement this contract later. */
public interface ExerciseQuestionGenerator {
    GeneratedExercise generate(LearningStep step);

    record GeneratedExercise(QuestionRequest question, String provider, boolean demo, String notice) {
    }
}
