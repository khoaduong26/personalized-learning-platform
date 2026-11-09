package com.tlcn.backend.academic.service;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class SubjectService {
    private final SubjectRepository repository;
    public SubjectService(SubjectRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Subject> findAll() { return repository.findAll(); }
    @Transactional public Subject save(Subject subject) { return repository.save(subject); }
}
