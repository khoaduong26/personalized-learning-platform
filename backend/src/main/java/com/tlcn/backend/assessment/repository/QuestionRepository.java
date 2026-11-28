package com.tlcn.backend.assessment.repository;

import com.tlcn.backend.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByLessonId(Long lessonId);

    boolean existsByLessonId(Long lessonId);

    List<Question> findByLessonIdOrderByIdDesc(Long lessonId);

    @Query("select q from Question q where q.lesson.id = :lessonId and (q.active = true or q.active is null) order by q.id desc")
    List<Question> findActiveByLessonIdOrderByIdDesc(@Param("lessonId") Long lessonId);
}
