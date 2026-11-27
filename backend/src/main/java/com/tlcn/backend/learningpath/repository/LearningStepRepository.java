package com.tlcn.backend.learningpath.repository;

import com.tlcn.backend.learningpath.entity.LearningStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LearningStepRepository extends JpaRepository<LearningStep, Long> {
    List<LearningStep> findByPathIdOrderByStepOrderAsc(Long pathId);

    Optional<LearningStep> findByIdAndPathId(Long id, Long pathId);

    boolean existsByLessonId(Long lessonId);
}
