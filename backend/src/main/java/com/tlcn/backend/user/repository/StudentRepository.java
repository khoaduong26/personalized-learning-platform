package com.tlcn.backend.user.repository;
import com.tlcn.backend.user.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRepository extends JpaRepository<Student, Long> {}
