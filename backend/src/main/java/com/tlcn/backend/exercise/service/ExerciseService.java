package com.tlcn.backend.exercise.service;

import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.exercise.entity.StepExercise;

import java.util.List;

public interface ExerciseService {
    List<StepExercise> forStep(Long stepId);

    List<Question> pendingReview();

    Question review(Long questionId, ApprovalStatus decision);

    StepExercise attach(StepExercise exercise);
}
