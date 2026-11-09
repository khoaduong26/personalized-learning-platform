package com.tlcn.backend.academic.controller;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.service.SubjectService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/subjects") public class SubjectController {
    private final SubjectService service;
    public SubjectController(SubjectService service) { this.service = service; }
    @GetMapping public List<Subject> list() { return service.findAll(); }
    @PostMapping public Subject create(@RequestBody Subject subject) { return service.save(subject); }
}
