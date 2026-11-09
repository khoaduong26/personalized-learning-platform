package com.tlcn.backend.learningpath.controller;
import com.tlcn.backend.learningpath.entity.*;
import com.tlcn.backend.learningpath.service.LearningPathService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/students/{studentId}/learning-paths")
public class LearningPathController {
    public record StepView(Long id, Integer order, String status, Double targetScore, Long lessonId) {
        static StepView from(LearningStep s) { return new StepView(s.getId(),s.getStepOrder(),s.getStatus().name(),s.getTargetScore(),s.getLesson().getId()); }
    }
    public record PathView(Long id, String status, Integer version, List<StepView> steps) {
        static PathView from(LearningPath p) { return new PathView(p.getId(),p.getStatus().name(),p.getVersion(),p.getSteps().stream().map(StepView::from).toList()); }
    }
    private final LearningPathService service;
    public LearningPathController(LearningPathService service) { this.service=service; }
    @GetMapping public List<PathView> list(@PathVariable Long studentId) { return service.forStudent(studentId).stream().map(PathView::from).toList(); }
    @GetMapping("/{pathId}") public PathView get(@PathVariable Long pathId) { return PathView.from(service.get(pathId)); }
}
