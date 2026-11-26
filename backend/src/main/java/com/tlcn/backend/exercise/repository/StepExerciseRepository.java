package com.tlcn.backend.exercise.repository;

import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StepExerciseRepository extends JpaRepository<StepExercise, Long> {
    List<StepExercise> findByStepId(Long stepId);

    Optional<StepExercise> findByQuestionId(Long questionId);

    @Query("select e.question from StepExercise e where " +
            "e.question.approvalStatus = com.tlcn.backend.assessment.entity.ApprovalStatus.PENDING " +
            "order by e.generatedAt desc")
    List<Question> findAllPendingReview();

    @Query("select e.question from StepExercise e where " +
            "e.question.approvalStatus = com.tlcn.backend.assessment.entity.ApprovalStatus.PENDING " +
            "and e.step.path.student.classroom.teacher.id = :teacherId order by e.generatedAt desc")
    List<Question> findPendingReviewForTeacher(@Param("teacherId") Long teacherId);
}
