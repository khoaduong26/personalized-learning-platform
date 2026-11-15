package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.service.TopicService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TopicServiceImpl implements TopicService {
    private final TopicRepository repository;

    public TopicServiceImpl(TopicRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Topic> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Topic> bySubject(Long id) {
        return repository.findBySubjectId(id);
    }

    @Transactional
    public Topic save(Topic topic) {
        return repository.save(topic);
    }
}
