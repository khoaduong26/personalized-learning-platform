package com.tlcn.backend.user.service;
import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.repository.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class TeacherService {
    private final TeacherRepository repository;
    public TeacherService(TeacherRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Teacher> findAll() { return repository.findAll(); }
    @Transactional public Teacher save(Teacher teacher) { return repository.save(teacher); }
}
