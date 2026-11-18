package com.tlcn.backend.learningpath.service;

import com.tlcn.backend.learningpath.entity.*;
import java.util.List;

public interface LearningPathService {
    List<LearningPath> forStudent(Long studentId);

    LearningPath get(Long id);

    List<LearningStep> steps(Long pathId);

    LearningPath save(LearningPath path);

    LearningStep updateStep(LearningStep step);
}
