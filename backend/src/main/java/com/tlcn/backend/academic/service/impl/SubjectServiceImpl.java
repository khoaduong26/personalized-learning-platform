package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import com.tlcn.backend.academic.service.SubjectService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository repository;
    private final TopicRepository topics;

    public SubjectServiceImpl(SubjectRepository repository, TopicRepository topics) {
        this.repository = repository;
        this.topics = topics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(subject -> new SubjectResponse(subject.getId(), subject.getName(),
                        (int) topics.countBySubjectId(subject.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse get(Long id) {
        Subject subject = requireSubject(id);
        return new SubjectResponse(subject.getId(), subject.getName(), (int) topics.countBySubjectId(id));
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        String name = request.name().strip();
        ensureUnique(name, null);
        Subject subject = repository.save(new Subject(name));
        return new SubjectResponse(subject.getId(), subject.getName(), 0);
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = requireSubject(id);
        String name = request.name().strip();
        ensureUnique(name, id);
        subject.setName(name);
        subject = repository.save(subject);
        return new SubjectResponse(subject.getId(), subject.getName(), (int) topics.countBySubjectId(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subject subject = requireSubject(id);
        if (topics.existsBySubjectId(id)) {
            throw new AcademicConflictException("Hãy xóa các chủ đề trước khi xóa môn học.");
        }
        repository.delete(subject);
    }

    private Subject requireSubject(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy môn học."));
    }

    private void ensureUnique(String name, Long exceptId) {
        if (repository.existsByNameIgnoreCase(name)
                && (exceptId == null || !repository.findById(exceptId)
                        .map(subject -> subject.getName().equalsIgnoreCase(name)).orElse(false))) {
            throw new AcademicConflictException("Tên môn học đã tồn tại.");
        }
    }
}
