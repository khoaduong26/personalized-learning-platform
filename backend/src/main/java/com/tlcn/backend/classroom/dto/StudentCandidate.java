package com.tlcn.backend.classroom.dto;

import com.tlcn.backend.user.entity.Student;

public record StudentCandidate(Long id, String fullName, String email) {
    public static StudentCandidate from(Student student) {
        return new StudentCandidate(student.getId(), student.getFullName(), student.getEmail());
    }
}
