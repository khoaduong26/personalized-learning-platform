package com.tlcn.backend.mastery.repository;
import com.tlcn.backend.mastery.entity.StudentError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudentErrorRepository extends JpaRepository<StudentError, Long> { List<StudentError> findByStudentIdOrderByCreatedAtDesc(Long studentId); }
