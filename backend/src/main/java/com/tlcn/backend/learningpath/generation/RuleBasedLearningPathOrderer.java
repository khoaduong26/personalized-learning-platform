package com.tlcn.backend.learningpath.generation;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.learning-path-generator", name = "provider", havingValue = "rules", matchIfMissing = true)
public class RuleBasedLearningPathOrderer implements LearningPathOrderer {
    @Override
    public List<Integer> order(List<Candidate> candidates) {
        return candidates.stream().map(Candidate::position).toList();
    }
}
