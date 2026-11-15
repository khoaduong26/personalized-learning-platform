package com.tlcn.backend.classroom.dto;
import com.tlcn.backend.user.entity.Student;
import java.time.LocalDate;
public record StudentSummary(Long id, String fullName, String email, String sex, LocalDate dateOfBirth) {
    public static StudentSummary from(Student student) {
        return new StudentSummary(student.getId(), student.getFullName(), student.getEmail(), student.getSex(), student.getDateOfBirth());
    }
}
