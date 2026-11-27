package com.tlcn.backend.mastery.repository;

import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface KnowledgeMasteryRepository extends JpaRepository<KnowledgeMastery, Long> {
    List<KnowledgeMastery> findByStudentId(Long studentId);

    @Query("select distinct m from KnowledgeMastery m left join m.student.additionalClassrooms extra "
            + "where m.student.classroom.id = :classroomId or extra.id = :classroomId")
    List<KnowledgeMastery> findByStudentClassroomId(@Param("classroomId") Long classroomId);

    Optional<KnowledgeMastery> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    boolean existsByLessonId(Long lessonId);
}
