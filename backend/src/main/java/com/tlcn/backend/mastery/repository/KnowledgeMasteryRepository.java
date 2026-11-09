package com.tlcn.backend.mastery.repository;
import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface KnowledgeMasteryRepository extends JpaRepository<KnowledgeMastery, Long> { List<KnowledgeMastery> findByStudentId(Long studentId); Optional<KnowledgeMastery> findByStudentIdAndLessonId(Long studentId, Long lessonId); }
