package com.tlcn.backend.assessment.repository;

import com.tlcn.backend.assessment.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByTeacherId(Long teacherId);

    List<Assessment> findByPublishedTrueOrderByCreatedAtDesc();

    List<Assessment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Optional<Assessment> findByIdAndTeacherId(Long id, Long teacherId);

    boolean existsByQuestions_Id(Long questionId);

    @Query("select distinct a from Assessment a join a.classrooms c join c.students s where a.published = true and s.id = :studentId order by a.createdAt desc")
    List<Assessment> findPublishedForStudent(@Param("studentId") Long studentId);
}
