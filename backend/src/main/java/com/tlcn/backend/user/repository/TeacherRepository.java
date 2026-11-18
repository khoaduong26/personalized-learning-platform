package com.tlcn.backend.user.repository;
import com.tlcn.backend.user.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TeacherRepository extends JpaRepository<Teacher, Long> { List<Teacher> findAllByOrderByFullNameAsc(); }
