package com.tlcn.backend.classroom.service;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class ClassroomService {
    private final ClassroomRepository repository;
    public ClassroomService(ClassroomRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true) public List<Classroom> findAll() { return repository.findAll(); }
    @Transactional(readOnly = true) public List<Classroom> findByTeacher(Long teacherId) { return repository.findByTeacherId(teacherId); }
    @Transactional public Classroom save(Classroom classroom) { return repository.save(classroom); }
}
