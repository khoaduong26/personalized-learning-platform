package com.tlcn.backend.classroom.controller;

import com.tlcn.backend.classroom.dto.*;
import com.tlcn.backend.classroom.service.ClassroomService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/classrooms")
public class ClassroomController {
    public record EnrollStudentRequest(@jakarta.validation.constraints.NotNull Long studentId) {}

    private final ClassroomService service;
    public ClassroomController(ClassroomService service) { this.service = service; }

    @GetMapping public List<ClassroomResponse> list(@AuthenticationPrincipal UserSummary actor) { return service.list(actor); }
    @GetMapping("/teachers") public List<TeacherOption> teachers(@AuthenticationPrincipal UserSummary actor) { return service.teachers(actor); }
    @GetMapping("/{id}") public ClassroomResponse get(@PathVariable Long id, @AuthenticationPrincipal UserSummary actor) { return service.get(id, actor); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ClassroomResponse create(@Valid @RequestBody ClassroomRequest request, @AuthenticationPrincipal UserSummary actor) { return service.create(request, actor); }
    @PutMapping("/{id}")
    public ClassroomResponse update(@PathVariable Long id, @Valid @RequestBody ClassroomRequest request,
            @AuthenticationPrincipal UserSummary actor) { return service.update(id, request, actor); }
    @GetMapping("/{id}/students")
    public List<StudentSummary> students(@PathVariable Long id, @RequestParam(required = false) @Size(max = 120) String search,
            @AuthenticationPrincipal UserSummary actor) { return service.students(id, search, actor); }
    @PostMapping("/{id}/students") @ResponseStatus(HttpStatus.CREATED)
    public StudentSummary addStudent(@PathVariable Long id, @Valid @RequestBody EnrollStudentRequest request,
            @AuthenticationPrincipal UserSummary actor) { return service.enroll(id, request.studentId(), actor); }
    @DeleteMapping("/{id}/students/{studentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable Long id, @PathVariable Long studentId,
            @AuthenticationPrincipal UserSummary actor) { service.removeStudent(id, studentId, actor); }
}
