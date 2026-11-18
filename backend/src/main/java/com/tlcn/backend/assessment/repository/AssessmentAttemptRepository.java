package com.tlcn.backend.assessment.repository;

import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    List<AssessmentAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    Optional<AssessmentAttempt> findByIdAndStudentId(Long id, Long studentId);
}
