package com.tlcn.backend.user.service;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class StudentService {
    private final StudentRepository repository;
    public StudentService(StudentRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Student> findAll() { return repository.findAll(); }
    @Transactional public Student save(Student student) { return repository.save(student); }
}
