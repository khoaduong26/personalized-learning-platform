package com.tlcn.backend.assessment.service.impl;

import java.time.Instant;

import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.AttemptStatus;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import com.tlcn.backend.assessment.repository.AssessmentAttemptRepository;
import com.tlcn.backend.assessment.repository.StudentAnswerRepository;
import com.tlcn.backend.assessment.service.AttemptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptServiceImpl implements AttemptService {
    private final AssessmentAttemptRepository attempts;
    private final StudentAnswerRepository answers;

    public AttemptServiceImpl(AssessmentAttemptRepository attempts, StudentAnswerRepository answers) {
        this.attempts = attempts;
        this.answers = answers;
    }

    @Transactional(readOnly = true)
    public AssessmentAttempt get(Long id) {
        return attempts.findById(id).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
    }

    @Transactional
    public AssessmentAttempt start(AssessmentAttempt attempt) {
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        return attempts.save(attempt);
    }

    @Transactional
    public StudentAnswer saveAnswer(StudentAnswer answer) {
        if (answer.getAttempt().getStatus() != AttemptStatus.IN_PROGRESS)
            throw new IllegalStateException("Attempt is already submitted");
        return answers.save(answer);
    }

    @Transactional
    public AssessmentAttempt submit(Long id) {
        AssessmentAttempt attempt = attempts.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS)
            throw new IllegalStateException("Attempt is already submitted");
        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        return attempts.save(attempt);
    }

}
