package com.tlcn.backend.mastery.controller;
import com.tlcn.backend.mastery.entity.*;
import com.tlcn.backend.mastery.service.MasteryService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/students/{studentId}/mastery")
public class MasteryController {
    public record MasteryView(Long id, Long lessonId, Double score, String level, Double confidence, Instant updatedAt) {
        static MasteryView from(KnowledgeMastery m) { return new MasteryView(m.getId(),m.getLesson().getId(),m.getMasteryScore(),m.getMasteryLevel().name(),m.getConfidenceScore(),m.getLastUpdated()); }
    }
    public record ErrorView(Long id, Long lessonId, String errorType, String description, Instant createdAt) {
        static ErrorView from(StudentError e) { return new ErrorView(e.getId(),e.getLesson().getId(),e.getErrorType(),e.getDescription(),e.getCreatedAt()); }
    }
    public record GapView(Long id, Long lessonId, String priority, String rootCause, String status, Instant identifiedAt) {
        static GapView from(KnowledgeGap g) { return new GapView(g.getId(),g.getLesson().getId(),g.getPriority().name(),g.getRootCause(),g.getStatus().name(),g.getIdentifiedAt()); }
    }
    private final MasteryService service;
    public MasteryController(MasteryService service) { this.service=service; }
    @GetMapping public List<MasteryView> profile(@PathVariable Long studentId) { return service.profile(studentId).stream().map(MasteryView::from).toList(); }
    @GetMapping("/errors") public List<ErrorView> errors(@PathVariable Long studentId) { return service.errors(studentId).stream().map(ErrorView::from).toList(); }
    @GetMapping("/gaps") public List<GapView> gaps(@PathVariable Long studentId) { return service.gaps(studentId).stream().map(GapView::from).toList(); }
}
