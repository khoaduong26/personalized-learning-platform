package com.tlcn.backend.classroom.repository;
import com.tlcn.backend.classroom.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClassroomRepository extends JpaRepository<Classroom, Long> { List<Classroom> findByTeacherId(Long teacherId); }
