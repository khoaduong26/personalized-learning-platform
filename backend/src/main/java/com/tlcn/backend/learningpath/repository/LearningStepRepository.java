package com.tlcn.backend.learningpath.repository;
import com.tlcn.backend.learningpath.entity.LearningStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LearningStepRepository extends JpaRepository<LearningStep, Long> { List<LearningStep> findByPathIdOrderByStepOrderAsc(Long pathId); }
