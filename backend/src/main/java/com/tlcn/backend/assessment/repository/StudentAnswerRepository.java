package com.tlcn.backend.assessment.repository;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> { List<StudentAnswer> findByAttemptId(Long attemptId); }
