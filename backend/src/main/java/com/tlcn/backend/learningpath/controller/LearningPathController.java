package com.tlcn.backend.learningpath.controller;

import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.service.LearningPathService;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/students/{studentId}/learning-paths")
public class LearningPathController {
    public record ClassroomView(Long id, String name, String schoolYear) {
        static ClassroomView from(Classroom classroom) {
            return new ClassroomView(classroom.getId(), classroom.getName(), classroom.getSchoolYear());
        }
    }

    public record StepView(Long id, Integer order, String status, Double targetScore, Long lessonId,
            Long topicId, String topicName, String lessonName, String lessonDescription) {
        static StepView from(LearningStep step) {
            return new StepView(step.getId(), step.getStepOrder(), step.getStatus().name(), step.getTargetScore(),
                    step.getLesson().getId(), step.getLesson().getTopic().getId(),
                    step.getLesson().getTopic().getName(), step.getLesson().getName(),
                    step.getLesson().getDescription());
        }
    }

    public record ProgressView(int total, int completed, int inProgress, int locked, int percentage) {
        static ProgressView from(List<LearningStep> steps) {
            int total = steps.size();
            int completed = (int) steps.stream().filter(step -> step.getStatus() == LearningStepStatus.COMPLETED).count();
            int inProgress = (int) steps.stream().filter(step -> step.getStatus() == LearningStepStatus.IN_PROGRESS).count();
            int locked = (int) steps.stream().filter(step -> step.getStatus() == LearningStepStatus.LOCKED).count();
            int percentage = total == 0 ? 0 : (int) Math.round(100.0 * completed / total);
            return new ProgressView(total, completed, inProgress, locked, percentage);
        }
    }

    public record PathView(Long id, String status, Integer version, Long classroomId, String classroomName,
            ProgressView progress, List<StepView> steps) {
        static PathView from(LearningPath path) {
            List<LearningStep> orderedSteps = path.getSteps().stream()
                    .sorted(java.util.Comparator.comparing(LearningStep::getStepOrder)).toList();
            return new PathView(path.getId(), path.getStatus().name(), path.getVersion(),
                    path.getClassroom() == null ? null : path.getClassroom().getId(),
                    path.getClassroom() == null ? "Chưa gắn lớp" : path.getClassroom().getName(),
                    ProgressView.from(orderedSteps), orderedSteps.stream().map(StepView::from).toList());
        }
    }

    public record UpdateStepStatusRequest(@NotNull LearningStepStatus status) {
    }

    private final LearningPathService service;

    public LearningPathController(LearningPathService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PathView> list(@PathVariable Long studentId,
            @RequestParam(required = false) Long classroomId,
            @AuthenticationPrincipal UserSummary actor) {
        return service.forStudent(studentId, classroomId, actor).stream().map(PathView::from).toList();
    }

    @GetMapping("/classrooms")
    @Transactional(readOnly = true)
    public List<ClassroomView> classrooms(@PathVariable Long studentId,
            @AuthenticationPrincipal UserSummary actor) {
        return service.classroomsForStudent(studentId, actor).stream().map(ClassroomView::from).toList();
    }

    @PostMapping("/generate")
    @Transactional
    public PathView generate(@PathVariable Long studentId,
            @RequestParam(required = false) Long classroomId,
            @AuthenticationPrincipal UserSummary actor) {
        return PathView.from(service.generate(studentId, classroomId, actor));
    }

    @GetMapping("/{pathId}")
    @Transactional(readOnly = true)
    public PathView get(@PathVariable Long studentId, @PathVariable Long pathId,
            @AuthenticationPrincipal UserSummary actor) {
        return PathView.from(service.get(studentId, pathId, actor));
    }

    @DeleteMapping("/{pathId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long studentId, @PathVariable Long pathId,
            @AuthenticationPrincipal UserSummary actor) {
        service.delete(studentId, pathId, actor);
    }

    @PatchMapping("/{pathId}/steps/{stepId}")
    @Transactional
    public PathView updateStep(@PathVariable Long studentId, @PathVariable Long pathId, @PathVariable Long stepId,
            @Valid @RequestBody UpdateStepStatusRequest request, @AuthenticationPrincipal UserSummary actor) {
        return PathView.from(service.updateStepStatus(studentId, pathId, stepId, request.status(), actor));
    }
}
