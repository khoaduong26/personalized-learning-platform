package com.tlcn.backend.user.service.impl;

import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.repository.StudentRepository;
import com.tlcn.backend.user.service.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StudentServiceImpl implements StudentService {
    private final StudentRepository repository;

    public StudentServiceImpl(StudentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return repository.findAll();
    }

    @Transactional
    public Student save(Student student) {
        return repository.save(student);
    }
}
