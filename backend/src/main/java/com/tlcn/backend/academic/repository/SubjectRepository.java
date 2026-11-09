package com.tlcn.backend.academic.repository;
import com.tlcn.backend.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SubjectRepository extends JpaRepository<Subject, Long> {}
