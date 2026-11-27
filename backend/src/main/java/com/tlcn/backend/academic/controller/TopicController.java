package com.tlcn.backend.academic.controller;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import com.tlcn.backend.academic.service.TopicService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService service;

    public TopicController(TopicService service) {
        this.service = service;
    }

    @GetMapping
    public List<TopicResponse> list(@RequestParam Long subjectId, @AuthenticationPrincipal UserSummary actor) {
        return service.bySubject(subjectId, actor);
    }

    @GetMapping("/{id}")
    public TopicResponse get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return service.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse create(@Valid @RequestBody TopicRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.create(request, actor);
    }

    @PutMapping("/{id}")
    public TopicResponse update(@PathVariable Long id, @Valid @RequestBody TopicRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        service.delete(id, actor);
    }
}
