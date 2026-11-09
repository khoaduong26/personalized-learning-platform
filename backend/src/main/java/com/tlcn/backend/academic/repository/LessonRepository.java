package com.tlcn.backend.academic.repository;
import com.tlcn.backend.academic.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LessonRepository extends JpaRepository<Lesson, Long> { List<Lesson> findByTopicId(Long topicId); }
