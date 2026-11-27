package com.tlcn.backend.academic.controller;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import com.tlcn.backend.academic.service.LessonService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService service;

    public LessonController(LessonService service) {
        this.service = service;
    }

    @GetMapping
    public List<LessonResponse> list(@RequestParam Long topicId, @AuthenticationPrincipal UserSummary actor) {
        return service.byTopic(topicId, actor);
    }

    @GetMapping("/{id}")
    public LessonResponse get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return service.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.create(request, actor);
    }

    @PutMapping("/{id}")
    public LessonResponse update(@PathVariable Long id, @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        service.delete(id, actor);
    }
}
