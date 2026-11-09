package com.tlcn.backend.assessment.service;
import com.tlcn.backend.assessment.entity.*;
import com.tlcn.backend.assessment.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service public class AttemptService {
    private final AssessmentAttemptRepository attempts; private final StudentAnswerRepository answers;
    public AttemptService(AssessmentAttemptRepository attempts, StudentAnswerRepository answers) { this.attempts=attempts; this.answers=answers; }
    @Transactional(readOnly=true) public AssessmentAttempt get(Long id) { return attempts.findById(id).orElseThrow(() -> new IllegalArgumentException("Attempt not found")); }
    @Transactional public AssessmentAttempt start(AssessmentAttempt attempt) { attempt.setStatus(AttemptStatus.IN_PROGRESS); return attempts.save(attempt); }
    @Transactional public StudentAnswer saveAnswer(StudentAnswer answer) {
        if (answer.getAttempt().getStatus() != AttemptStatus.IN_PROGRESS) throw new IllegalStateException("Attempt is already submitted");
        return answers.save(answer);
    }
    @Transactional public AssessmentAttempt submit(Long id) {
        AssessmentAttempt attempt = attempts.findById(id).orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) throw new IllegalStateException("Attempt is already submitted");
        attempt.setSubmittedAt(Instant.now()); attempt.setStatus(AttemptStatus.SUBMITTED); return attempts.save(attempt);
    }
}
