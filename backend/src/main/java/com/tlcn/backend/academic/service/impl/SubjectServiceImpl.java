package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.service.SubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tlcn.backend.academic.entity.Subject;
import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository repository;

    public SubjectServiceImpl(SubjectRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Subject> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Subject save(Subject subject) {
        return repository.save(subject);
    }
}
