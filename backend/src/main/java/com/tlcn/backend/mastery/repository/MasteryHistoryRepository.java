package com.tlcn.backend.mastery.repository;
import com.tlcn.backend.mastery.entity.MasteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MasteryHistoryRepository extends JpaRepository<MasteryHistory, Long> { List<MasteryHistory> findByMasteryIdOrderByRecordedAtAsc(Long masteryId); }
