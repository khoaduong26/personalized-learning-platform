package com.tlcn.backend.assessment.service;

import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import com.tlcn.backend.assessment.dto.AnswerRequest;
import com.tlcn.backend.user.dto.UserSummary;

public interface AttemptService {
    AssessmentAttempt get(Long id, UserSummary actor);

    AssessmentAttempt start(Long assessmentId, UserSummary actor);

    StudentAnswer saveAnswer(Long attemptId, AnswerRequest answer, UserSummary actor);

    AssessmentAttempt submit(Long id, UserSummary actor);
}
