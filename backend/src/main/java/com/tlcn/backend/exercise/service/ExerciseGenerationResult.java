package com.tlcn.backend.exercise.service;

import com.tlcn.backend.assessment.entity.Question;

public record ExerciseGenerationResult(Question question, String provider, boolean demo, String notice) {
}
