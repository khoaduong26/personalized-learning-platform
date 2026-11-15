package com.tlcn.backend.classroom.service;

import com.tlcn.backend.classroom.dto.*;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;

public interface ClassroomService {
    List<ClassroomResponse> list(UserSummary actor);

    ClassroomResponse get(Long id, UserSummary actor);

    ClassroomResponse create(ClassroomRequest request, UserSummary actor);

    ClassroomResponse update(Long id, ClassroomRequest request, UserSummary actor);

    List<StudentSummary> students(Long classroomId, String search, UserSummary actor);

    List<TeacherOption> teachers(UserSummary actor);

    List<StudentCandidate> findCandidate(String email, UserSummary actor);

    StudentSummary enroll(Long classroomId, Long studentId, UserSummary actor);

    StudentSummary getStudent(Long studentId, UserSummary actor);

    void removeStudent(Long classroomId, Long studentId, UserSummary actor);

    StudentSummary updateStudent(Long studentId, StudentUpdateRequest request, UserSummary actor);

}
