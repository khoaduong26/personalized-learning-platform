package com.tlcn.backend.academic.service;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class LessonService {
    private final LessonRepository repository;
    public LessonService(LessonRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Lesson> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public List<Lesson> byTopic(Long id) { return repository.findByTopicId(id); }
    @Transactional public Lesson save(Lesson lesson) { return repository.save(lesson); }
}
