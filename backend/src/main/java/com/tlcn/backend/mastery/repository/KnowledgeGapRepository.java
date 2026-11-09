package com.tlcn.backend.mastery.repository;
import com.tlcn.backend.mastery.entity.KnowledgeGap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, Long> { List<KnowledgeGap> findByStudentId(Long studentId); }
