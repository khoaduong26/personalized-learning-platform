package com.tlcn.backend.learningpath.repository;
import com.tlcn.backend.learningpath.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LearningPathRepository extends JpaRepository<LearningPath, Long> { List<LearningPath> findByStudentIdOrderByCreatedAtDesc(Long studentId); }
