package com.tlcn.backend.learningpath.repository;

import com.tlcn.backend.learningpath.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    @EntityGraph(attributePaths = {"steps", "steps.lesson", "classroom"})
    List<LearningPath> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"steps", "steps.lesson", "classroom"})
    Optional<LearningPath> findByIdAndStudentId(Long id, Long studentId);

    @Query("select coalesce(max(p.version), 0) from LearningPath p where p.student.id = :studentId")
    int findMaxVersionByStudentId(@Param("studentId") Long studentId);

    @EntityGraph(attributePaths = {"steps", "steps.lesson", "classroom"})
    List<LearningPath> findByStudentIdAndClassroomIdOrderByCreatedAtDesc(Long studentId, Long classroomId);

    @Query("select coalesce(max(p.version), 0) from LearningPath p "
            + "where p.student.id = :studentId and p.classroom.id = :classroomId")
    int findMaxVersionByStudentIdAndClassroomId(@Param("studentId") Long studentId,
            @Param("classroomId") Long classroomId);
}
