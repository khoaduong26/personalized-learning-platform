package com.tlcn.backend.academic.controller;

import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import com.tlcn.backend.academic.service.SubjectService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping
    public List<SubjectResponse> list(@AuthenticationPrincipal UserSummary actor) {
        return service.findAll(actor);
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return service.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse create(@Valid @RequestBody SubjectRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.create(request, actor);
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request,
            @AuthenticationPrincipal UserSummary actor) {
        return service.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        service.delete(id, actor);
    }

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectResponse copy(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) {
        return service.copy(id, actor);
    }
}
