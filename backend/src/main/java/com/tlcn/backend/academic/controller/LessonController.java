package com.tlcn.backend.academic.controller;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import com.tlcn.backend.academic.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService service;
    public LessonController(LessonService service) { this.service = service; }

    @GetMapping public List<LessonResponse> list(@RequestParam Long topicId) { return service.byTopic(topicId); }
    @GetMapping("/{id}") public LessonResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@Valid @RequestBody LessonRequest request) { return service.create(request); }
    @PutMapping("/{id}") public LessonResponse update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
