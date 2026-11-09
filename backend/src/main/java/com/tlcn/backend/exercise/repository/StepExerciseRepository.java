package com.tlcn.backend.exercise.repository;
import com.tlcn.backend.exercise.entity.StepExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StepExerciseRepository extends JpaRepository<StepExercise, Long> { List<StepExercise> findByStepId(Long stepId); }
