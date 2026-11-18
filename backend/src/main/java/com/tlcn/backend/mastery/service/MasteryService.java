package com.tlcn.backend.mastery.service;

import com.tlcn.backend.mastery.entity.*;
import java.util.List;

public interface MasteryService {
    List<KnowledgeMastery> profile(Long studentId);

    List<MasteryHistory> history(Long masteryId);

    List<StudentError> errors(Long studentId);

    List<KnowledgeGap> gaps(Long studentId);

    KnowledgeMastery save(KnowledgeMastery value);

    StudentError save(StudentError value);

    KnowledgeGap save(KnowledgeGap value);
}
