package com.tlcn.backend.learningpath.service;

import com.tlcn.backend.learningpath.entity.*;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;

public interface LearningPathService {
    List<LearningPath> forStudent(Long studentId, UserSummary actor);

    LearningPath get(Long studentId, Long pathId, UserSummary actor);

    LearningPath generate(Long studentId, UserSummary actor);

    LearningPath updateStepStatus(Long studentId, Long pathId, Long stepId, LearningStepStatus status,
            UserSummary actor);
}
