package com.tlcn.backend.mastery.service.impl;

import com.tlcn.backend.mastery.entity.KnowledgeGap;
import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import com.tlcn.backend.mastery.entity.MasteryHistory;
import com.tlcn.backend.mastery.entity.StudentError;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.MasteryHistoryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.mastery.service.MasteryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class MasteryServiceImpl implements MasteryService {
    private final KnowledgeMasteryRepository mastery;
    private final MasteryHistoryRepository history;
    private final StudentErrorRepository errors;
    private final KnowledgeGapRepository gaps;

    public MasteryServiceImpl(KnowledgeMasteryRepository mastery, MasteryHistoryRepository history,
            StudentErrorRepository errors, KnowledgeGapRepository gaps) {
        this.mastery = mastery;
        this.history = history;
        this.errors = errors;
        this.gaps = gaps;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeMastery> profile(Long studentId) {
        return mastery.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<MasteryHistory> history(Long masteryId) {
        return history.findByMasteryIdOrderByRecordedAtAsc(masteryId);
    }

    @Transactional(readOnly = true)
    public List<StudentError> errors(Long studentId) {
        return errors.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeGap> gaps(Long studentId) {
        return gaps.findByStudentId(studentId);
    }

    @Transactional
    public KnowledgeMastery save(KnowledgeMastery value) {
        return mastery.save(value);
    }

    @Transactional
    public StudentError save(StudentError value) {
        return errors.save(value);
    }

    @Transactional
    public KnowledgeGap save(KnowledgeGap value) {
        return gaps.save(value);
    }
}
