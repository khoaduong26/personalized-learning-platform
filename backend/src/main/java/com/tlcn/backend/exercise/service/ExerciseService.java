package com.tlcn.backend.exercise.service;
import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.exercise.entity.StepExercise;
import com.tlcn.backend.exercise.repository.StepExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service public class ExerciseService {
    private final StepExerciseRepository exercises; private final QuestionRepository questions;
    public ExerciseService(StepExerciseRepository exercises, QuestionRepository questions) { this.exercises=exercises; this.questions=questions; }
    @Transactional(readOnly=true) public List<StepExercise> forStep(Long stepId) { return exercises.findByStepId(stepId).stream().filter(e -> e.getQuestion().getApprovalStatus()==ApprovalStatus.APPROVED).toList(); }
    @Transactional(readOnly=true) public List<Question> pendingReview() { return questions.findAll().stream().filter(q -> q.isAiGenerated() && q.getApprovalStatus()==ApprovalStatus.PENDING).toList(); }
    @Transactional public Question review(Long questionId, ApprovalStatus decision) {
        if (decision == ApprovalStatus.PENDING) throw new IllegalArgumentException("Review decision must be APPROVED or REJECTED");
        Question question=questions.findById(questionId).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (!question.isAiGenerated()) throw new IllegalStateException("Only AI-generated questions can be reviewed");
        question.setApprovalStatus(decision); return questions.save(question);
    }
    @Transactional public StepExercise attach(StepExercise exercise) {
        exercise.getQuestion().setAiGenerated(true); exercise.getQuestion().setApprovalStatus(ApprovalStatus.PENDING);
        return exercises.save(exercise);
    }
}
