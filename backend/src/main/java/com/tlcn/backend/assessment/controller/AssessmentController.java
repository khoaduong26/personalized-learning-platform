package com.tlcn.backend.assessment.controller;

import com.tlcn.backend.assessment.dto.*;
import com.tlcn.backend.assessment.service.AssessmentService;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {
    public record ClassroomOption(Long id, String name, String schoolYear) {
        static ClassroomOption from(Classroom c) {
            return new ClassroomOption(c.getId(), c.getName(), c.getSchoolYear());
        }
    }

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssessmentView> list(@AuthenticationPrincipal UserSummary actor) {
        return service.list(actor).stream().map(AssessmentView::from).toList();
    }

    @GetMapping("/{id}")
    public AssessmentView get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return AssessmentView.from(service.get(id, actor));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentView create(@Valid @RequestBody AssessmentRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return AssessmentView.from(service.save(request, actor));
    }

    @PostMapping("/{id}/publish")
    public AssessmentView publish(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return AssessmentView.from(service.publish(id, actor));
    }

    @GetMapping("/questions")
    public List<QuestionView> questions(@RequestParam Long lessonId, @AuthenticationPrincipal UserSummary actor) {
        return service.questionsForLesson(lessonId, actor).stream().map(QuestionView::from).toList();
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionView createQuestion(@Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return QuestionView.from(service.createQuestion(request, actor));
    }

    @GetMapping("/classrooms")
    public List<ClassroomOption> classrooms(@AuthenticationPrincipal UserSummary actor) {
        return service.classroomsForTeacher(actor).stream().map(ClassroomOption::from).toList();
    }
}
