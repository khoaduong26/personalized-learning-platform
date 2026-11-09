package com.tlcn.backend.assessment.controller;
import com.tlcn.backend.assessment.dto.*;
import com.tlcn.backend.assessment.service.AssessmentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/assessments")
public class AssessmentController {
    private final AssessmentService service;
    public AssessmentController(AssessmentService service) { this.service=service; }
    @GetMapping public List<AssessmentView> list(@RequestParam(required=false) Long teacherId) {
        return (teacherId==null?service.list():service.byTeacher(teacherId)).stream().map(AssessmentView::from).toList();
    }
    @GetMapping("/{id}") public AssessmentView get(@PathVariable Long id) { return AssessmentView.from(service.get(id)); }
    @GetMapping("/questions") public List<QuestionView> questions(@RequestParam Long lessonId) { return service.questionsForLesson(lessonId).stream().map(QuestionView::from).toList(); }
}
