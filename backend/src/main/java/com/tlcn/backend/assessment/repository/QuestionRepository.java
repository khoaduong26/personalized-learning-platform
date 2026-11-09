package com.tlcn.backend.assessment.repository;
import com.tlcn.backend.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface QuestionRepository extends JpaRepository<Question, Long> { List<Question> findByLessonId(Long lessonId); }
