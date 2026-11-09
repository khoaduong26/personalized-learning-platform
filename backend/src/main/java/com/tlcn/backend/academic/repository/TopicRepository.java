package com.tlcn.backend.academic.repository;
import com.tlcn.backend.academic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TopicRepository extends JpaRepository<Topic, Long> { List<Topic> findBySubjectId(Long subjectId); }
