package com.tlcn.backend.exercise.controller;
import com.tlcn.backend.assessment.dto.QuestionView;
import com.tlcn.backend.assessment.entity.ApprovalStatus;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.exercise.service.ExerciseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/exercises")
public class ExerciseController {
    public record ReviewRequest(ApprovalStatus status) {}
    public record ReviewView(Long questionId, String content, String status) {
        static ReviewView from(Question q) { return new ReviewView(q.getId(),q.getContent(),q.getApprovalStatus().name()); }
    }
    private final ExerciseService service;
    public ExerciseController(ExerciseService service) { this.service=service; }
    @GetMapping("/steps/{stepId}") public List<QuestionView> forStep(@PathVariable Long stepId) {
        return service.forStep(stepId).stream().map(e -> QuestionView.from(e.getQuestion())).toList();
    }
    @GetMapping("/pending-review") public List<ReviewView> pending() { return service.pendingReview().stream().map(ReviewView::from).toList(); }
    @PatchMapping("/questions/{questionId}/review") public ReviewView review(@PathVariable Long questionId, @RequestBody ReviewRequest request) {
        return ReviewView.from(service.review(questionId, request.status()));
    }
}
