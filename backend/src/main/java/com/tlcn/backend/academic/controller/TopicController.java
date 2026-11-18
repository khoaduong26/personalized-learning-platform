package com.tlcn.backend.academic.controller;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import com.tlcn.backend.academic.service.TopicService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/topics")
public class TopicController {
    private final TopicService service;
    public TopicController(TopicService service) { this.service = service; }

    @GetMapping public List<TopicResponse> list(@RequestParam Long subjectId) { return service.bySubject(subjectId); }
    @GetMapping("/{id}") public TopicResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse create(@Valid @RequestBody TopicRequest request) { return service.create(request); }
    @PutMapping("/{id}") public TopicResponse update(@PathVariable Long id, @Valid @RequestBody TopicRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}
