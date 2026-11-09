package com.tlcn.backend.classroom.controller;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.service.ClassroomService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/classrooms")
public class ClassroomController {
    private final ClassroomService service;
    public ClassroomController(ClassroomService service) { this.service = service; }
    @GetMapping public List<Classroom> list(@RequestParam(required = false) Long teacherId) { return teacherId == null ? service.findAll() : service.findByTeacher(teacherId); }
}
