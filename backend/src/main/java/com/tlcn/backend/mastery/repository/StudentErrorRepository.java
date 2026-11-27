package com.tlcn.backend.mastery.repository;

import com.tlcn.backend.mastery.entity.StudentError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentErrorRepository extends JpaRepository<StudentError, Long> {
    List<StudentError> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    boolean existsByAnswerId(Long answerId);

    long countByStudentIdAndLessonId(Long studentId, Long lessonId);

    boolean existsByLessonId(Long lessonId);
}
