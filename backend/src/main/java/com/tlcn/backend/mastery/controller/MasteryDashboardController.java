package com.tlcn.backend.mastery.controller;

import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mastery/classrooms/{classroomId}/dashboard")
public class MasteryDashboardController {
    public record LessonSummary(Long lessonId, double averageMastery, long studentsTracked,
            long studentsNeedingReview) {}

    private final MasteryService service;

    public MasteryDashboardController(MasteryService service) {
        this.service = service;
    }

    @GetMapping
    public List<LessonSummary> dashboard(@PathVariable Long classroomId,
            @AuthenticationPrincipal UserSummary actor) {
        return service.classroomDashboard(classroomId, actor).stream()
                .collect(java.util.stream.Collectors.groupingBy(value -> value.getLesson().getId()))
                .entrySet().stream()
                .map(entry -> new LessonSummary(entry.getKey(),
                        entry.getValue().stream().mapToDouble(value -> value.getMasteryScore()).average().orElse(0),
                        entry.getValue().size(),
                        entry.getValue().stream().filter(value -> value.getMasteryScore() < 70).count()))
                .toList();
    }
}
