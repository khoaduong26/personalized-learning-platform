package com.tlcn.backend.user.repository;
import com.tlcn.backend.user.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TeacherRepository extends JpaRepository<Teacher, Long> {}
