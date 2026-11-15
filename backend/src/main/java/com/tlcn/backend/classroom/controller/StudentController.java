package com.tlcn.backend.classroom.controller;

import com.tlcn.backend.classroom.dto.StudentCandidate;
import com.tlcn.backend.classroom.dto.StudentSummary;
import com.tlcn.backend.classroom.dto.StudentUpdateRequest;
import com.tlcn.backend.classroom.service.ClassroomService;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/students")
public class StudentController {
    private final ClassroomService service;
    public StudentController(ClassroomService service) { this.service = service; }

    @GetMapping("/candidates")
    public List<StudentCandidate> candidates(@RequestParam @NotBlank @Email String email, @AuthenticationPrincipal UserSummary actor) {
        return service.findCandidate(email, actor);
    }

    @GetMapping("/{studentId}")
    public StudentSummary get(@PathVariable Long studentId, @AuthenticationPrincipal UserSummary actor) {
        return service.getStudent(studentId, actor);
    }

    @PatchMapping("/{studentId}")
    public StudentSummary update(@PathVariable Long studentId, @Valid @RequestBody StudentUpdateRequest request,
            @AuthenticationPrincipal UserSummary actor) { return service.updateStudent(studentId, request, actor); }
}
