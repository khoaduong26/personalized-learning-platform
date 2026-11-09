package com.tlcn.backend.academic.controller;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.service.LessonService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/lessons") public class LessonController {
    private final LessonService service;
    public LessonController(LessonService service) { this.service = service; }
    @GetMapping public List<Lesson> list(@RequestParam(required = false) Long topicId) { return topicId == null ? service.findAll() : service.byTopic(topicId); }
    @PostMapping public Lesson create(@RequestBody Lesson lesson) { return service.save(lesson); }
}
