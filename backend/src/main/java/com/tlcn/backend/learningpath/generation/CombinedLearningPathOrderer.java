package com.tlcn.backend.learningpath.generation;

import com.tlcn.backend.learningpath.generation.LearningPathOrderer.Candidate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnExpression("'${APP_LEARNING_PATH_GENERATOR_PROVIDER:rules}' == 'combined'")
public class CombinedLearningPathOrderer implements LearningPathOrderer {
    private final OpenAiLearningPathOrderer generator;
    private final LearningPathOrderReviewer reviewer;

    public CombinedLearningPathOrderer(OpenAiLearningPathOrderer generator, LearningPathOrderReviewer reviewer) {
        this.generator = generator;
        this.reviewer = reviewer;
    }

    @Override
    public List<Integer> order(List<Candidate> candidates) {
        List<Integer> proposedOrder = generator.order(candidates);
        LearningPathOrderReviewer.ReviewResult review = reviewer.review(candidates, proposedOrder);
        if (review == null || !review.accepted()) {
            throw new IllegalStateException("Gemini chưa chấp nhận thứ tự lộ trình.");
        }
        return proposedOrder;
    }
}
