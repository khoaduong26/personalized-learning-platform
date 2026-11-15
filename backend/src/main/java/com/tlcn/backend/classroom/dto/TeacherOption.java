package com.tlcn.backend.classroom.dto;
import com.tlcn.backend.user.entity.Teacher;
public record TeacherOption(Long id, String fullName, String email) {
    public static TeacherOption from(Teacher teacher) { return new TeacherOption(teacher.getId(), teacher.getFullName(), teacher.getEmail()); }
}
