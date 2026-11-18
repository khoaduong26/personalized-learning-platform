package com.tlcn.backend.learningpath.service.impl;

import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.repository.LearningPathRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.learningpath.service.LearningPathService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    private final LearningPathRepository paths;
    private final LearningStepRepository steps;

    public LearningPathServiceImpl(LearningPathRepository paths, LearningStepRepository steps) {
        this.paths = paths;
        this.steps = steps;
    }

    @Transactional(readOnly = true)
    public List<LearningPath> forStudent(Long studentId) {
        return paths.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public LearningPath get(Long id) {
        return paths.findById(id).orElseThrow(() -> new IllegalArgumentException("Learning path not found"));
    }

    @Transactional(readOnly = true)
    public List<LearningStep> steps(Long pathId) {
        return steps.findByPathIdOrderByStepOrderAsc(pathId);
    }

    @Transactional
    public LearningPath save(LearningPath path) {
        return paths.save(path);
    }

    @Transactional
    public LearningStep updateStep(LearningStep step) {
        return steps.save(step);
    }
}
