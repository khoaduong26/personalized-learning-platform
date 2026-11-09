package com.tlcn.backend.assessment.controller;
import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.assessment.service.AttemptService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/attempts")
public class AttemptController {
    public record AttemptResult(Long id, Long assessmentId, Long studentId, String status, java.time.Instant submittedAt, double totalScore) {
        static AttemptResult from(AssessmentAttempt a) { return new AttemptResult(a.getId(), a.getAssessment()==null?null:a.getAssessment().getId(), a.getStudent().getId(), a.getStatus().name(), a.getSubmittedAt(), a.totalScore()); }
    }
    private final AttemptService attempts; private final AssessmentService assessments;
    public AttemptController(AttemptService attempts, AssessmentService assessments) { this.attempts=attempts; this.assessments=assessments; }
    @GetMapping("/student/{studentId}") public List<AttemptResult> byStudent(@PathVariable Long studentId) { return assessments.attemptsForStudent(studentId).stream().map(AttemptResult::from).toList(); }
    @GetMapping("/{id}") public AttemptResult get(@PathVariable Long id) { return AttemptResult.from(attempts.get(id)); }
    @PostMapping("/{id}/submit") public AttemptResult submit(@PathVariable Long id) { return AttemptResult.from(attempts.submit(id)); }
}
