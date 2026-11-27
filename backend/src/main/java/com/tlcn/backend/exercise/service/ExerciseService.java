package com.tlcn.backend.exercise.service;

import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.dto.QuestionRequest;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.user.dto.UserSummary;

import java.util.List;

public interface ExerciseService {
    List<StepExercise> forStep(Long stepId, UserSummary actor);

    List<StepExercise> pendingReview(UserSummary actor);

    Question createGenerated(Long stepId, QuestionRequest request, UserSummary actor);

    Question createManual(Long stepId, QuestionRequest request, UserSummary actor);

    ExerciseGenerationResult generateForStep(Long stepId, UserSummary actor);

    Question updateGenerated(Long questionId, QuestionRequest request, UserSummary actor);

    Question review(Long questionId, ApprovalStatus decision, UserSummary actor);

    StepExercise attach(StepExercise exercise);
}
