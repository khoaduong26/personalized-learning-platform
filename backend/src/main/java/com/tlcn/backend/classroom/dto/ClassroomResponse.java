package com.tlcn.backend.classroom.dto;
import com.tlcn.backend.classroom.entity.Classroom;
public record ClassroomResponse(Long id, String name, String schoolYear, Long teacherId, String teacherName, int studentCount) {
    public static ClassroomResponse from(Classroom classroom, int studentCount) {
        return new ClassroomResponse(classroom.getId(), classroom.getName(), classroom.getSchoolYear(),
                classroom.getTeacher().getId(), classroom.getTeacher().getFullName(), studentCount);
    }
}
