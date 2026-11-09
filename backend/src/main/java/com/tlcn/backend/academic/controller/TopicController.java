package com.tlcn.backend.academic.controller;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.service.TopicService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/topics") public class TopicController {
    private final TopicService service;
    public TopicController(TopicService service) { this.service = service; }
    @GetMapping public List<Topic> list(@RequestParam(required = false) Long subjectId) { return subjectId == null ? service.findAll() : service.bySubject(subjectId); }
    @PostMapping public Topic create(@RequestBody Topic topic) { return service.save(topic); }
}
