package com.tlcn.backend.user.service.impl;

import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.repository.TeacherRepository;
import com.tlcn.backend.user.service.TeacherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {
    private final TeacherRepository repository;

    public TeacherServiceImpl(TeacherRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Teacher save(Teacher teacher) {
        return repository.save(teacher);
    }
}
