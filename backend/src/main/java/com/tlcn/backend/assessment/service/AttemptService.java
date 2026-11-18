package com.tlcn.backend.assessment.service;

import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.StudentAnswer;

public interface AttemptService {
    AssessmentAttempt get(Long id);

    AssessmentAttempt start(AssessmentAttempt attempt);

    StudentAnswer saveAnswer(StudentAnswer answer);

    AssessmentAttempt submit(Long id);
}
