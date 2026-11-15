package com.tlcn.backend.classroom.service;

import com.tlcn.backend.classroom.dto.*;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.entity.UserStatus;
import com.tlcn.backend.user.repository.StudentRepository;
import com.tlcn.backend.user.repository.TeacherRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ClassroomService {
    private final ClassroomRepository classrooms;
    private final TeacherRepository teachers;
    private final StudentRepository students;

    public ClassroomService(ClassroomRepository classrooms, TeacherRepository teachers, StudentRepository students) {
        this.classrooms = classrooms;
        this.teachers = teachers;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> list(UserSummary actor) {
        List<Classroom> items = actor.role() == Role.ADMIN
                ? classrooms.findAllByOrderByNameAsc()
                : classrooms.findByTeacherIdOrderByNameAsc(actor.id());
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClassroomResponse get(Long id, UserSummary actor) { return toResponse(requireAccessible(id, actor)); }

    @Transactional
    public ClassroomResponse create(ClassroomRequest request, UserSummary actor) {
        String name = normalize(request.name(), "Tên lớp không được để trống.");
        String year = validateSchoolYear(request.schoolYear());
        Teacher owner = ownerForCreate(request.teacherId(), actor);
        Classroom classroom = classrooms.save(new Classroom(name, year, owner));
        return toResponse(classroom);
    }

    @Transactional
    public ClassroomResponse update(Long id, ClassroomRequest request, UserSummary actor) {
        Classroom classroom = requireAccessible(id, actor);
        classroom.setName(normalize(request.name(), "Tên lớp không được để trống."));
        classroom.setSchoolYear(validateSchoolYear(request.schoolYear()));
        return toResponse(classrooms.save(classroom));
    }

    @Transactional(readOnly = true)
    public List<StudentSummary> students(Long classroomId, String search, UserSummary actor) {
        requireAccessible(classroomId, actor);
        String query = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        return students.findByClassroomIdOrderByFullNameAsc(classroomId).stream()
                .filter(student -> query.isEmpty() || student.getFullName().toLowerCase(Locale.ROOT).contains(query)
                        || student.getEmail().toLowerCase(Locale.ROOT).contains(query))
                .map(StudentSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherOption> teachers(UserSummary actor) {
        requireAdmin(actor);
        return teachers.findAllByOrderByFullNameAsc().stream().map(TeacherOption::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentCandidate> findCandidate(String email, UserSummary actor) {
        requireStaff(actor);
        String normalizedEmail = normalize(email, "Vui lòng nhập email học sinh.").toLowerCase(Locale.ROOT);
        return students.findByEmailIgnoreCaseAndClassroomIsNullAndStatus(normalizedEmail, UserStatus.ACTIVE)
                .map(StudentCandidate::from).stream().toList();
    }

    @Transactional
    public StudentSummary enroll(Long classroomId, Long studentId, UserSummary actor) {
        Classroom classroom = requireAccessible(classroomId, actor);
        Student student = students.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy học sinh."));
        if (student.getStatus() != UserStatus.ACTIVE) {
            throw new ClassroomConflictException("Chỉ có thể thêm học sinh đã kích hoạt tài khoản.");
        }
        if (student.getClassroom() != null && !student.getClassroom().getId().equals(classroomId)) {
            throw new ClassroomConflictException("Học sinh đã thuộc một lớp khác.");
        }
        student.setClassroom(classroom);
        return StudentSummary.from(students.save(student));
    }

    @Transactional(readOnly = true)
    public StudentSummary getStudent(Long studentId, UserSummary actor) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy học sinh."));
        Classroom classroom = student.getClassroom();
        if (classroom == null) throw new ClassroomNotFoundException("Học sinh chưa thuộc lớp nào.");
        requireAccessible(classroom.getId(), actor);
        return StudentSummary.from(student);
    }

    @Transactional
    public void removeStudent(Long classroomId, Long studentId, UserSummary actor) {
        requireAccessible(classroomId, actor);
        Student student = students.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy học sinh."));
        if (student.getClassroom() == null || !student.getClassroom().getId().equals(classroomId)) {
            throw new ClassroomNotFoundException("Học sinh không thuộc lớp này.");
        }
        student.setClassroom(null);
        students.save(student);
    }

    @Transactional
    public StudentSummary updateStudent(Long studentId, StudentUpdateRequest request, UserSummary actor) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy học sinh."));
        Classroom classroom = student.getClassroom();
        if (classroom == null) throw new ClassroomNotFoundException("Học sinh chưa thuộc lớp nào.");
        requireAccessible(classroom.getId(), actor);
        student.setFullName(normalize(request.fullName(), "Tên học sinh không được để trống."));
        student.setSex(request.sex() == null || request.sex().isBlank() ? null : request.sex().strip());
        student.setDateOfBirth(request.dateOfBirth());
        return StudentSummary.from(students.save(student));
    }

    private ClassroomResponse toResponse(Classroom classroom) {
        return ClassroomResponse.from(classroom, Math.toIntExact(students.countByClassroomId(classroom.getId())));
    }

    private Classroom requireAccessible(Long classroomId, UserSummary actor) {
        if (actor.role() == Role.ADMIN) {
            return classrooms.findById(classroomId).orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy lớp học."));
        }
        return classrooms.findByIdAndTeacherId(classroomId, actor.id())
                .orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy lớp học."));
    }

    private Teacher ownerForCreate(Long requestedTeacherId, UserSummary actor) {
        Long teacherId = actor.role() == Role.ADMIN ? requestedTeacherId : actor.id();
        if (teacherId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cần chọn giáo viên phụ trách lớp.");
        return teachers.findById(teacherId).orElseThrow(() -> new ClassroomNotFoundException("Không tìm thấy giáo viên."));
    }

    private void requireAdmin(UserSummary actor) {
        if (actor.role() != Role.ADMIN) throw new ClassroomNotFoundException("Không tìm thấy dữ liệu.");
    }

    private void requireStaff(UserSummary actor) {
        if (actor.role() != Role.ADMIN && actor.role() != Role.TEACHER) throw new ClassroomNotFoundException("Không tìm thấy dữ liệu.");
    }

    private String normalize(String value, String message) {
        if (value == null || value.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return value.strip();
    }

    private String validateSchoolYear(String value) {
        String year = normalize(value, "Vui lòng nhập năm học.");
        if (!year.matches("\\d{4}-\\d{4}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Năm học cần theo dạng YYYY-YYYY.");
        int start = Integer.parseInt(year.substring(0, 4));
        int end = Integer.parseInt(year.substring(5, 9));
        if (end != start + 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Năm học phải gồm hai năm liên tiếp.");
        return year;
    }
}
