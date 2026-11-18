package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.service.LessonService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository repository;

    public LessonServiceImpl(LessonRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Lesson> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Lesson> byTopic(Long id) {
        return repository.findByTopicId(id);
    }

    @Transactional
    public Lesson save(Lesson lesson) {
        return repository.save(lesson);
    }
}
