package com.tlcn.backend.classroom.entity;

import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.Teacher;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "classrooms")
public class Classroom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 20) private String schoolYear;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "teacher_id") private Teacher teacher;
    @ManyToMany @JoinTable(name = "classroom_students", joinColumns = @JoinColumn(name = "classroom_id"), inverseJoinColumns = @JoinColumn(name = "student_id")) private Set<Student> students = new HashSet<>();
    protected Classroom() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSchoolYear() { return schoolYear; }
    public void setSchoolYear(String schoolYear) { this.schoolYear = schoolYear; }
    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }
    public Set<Student> getStudents() { return students; }
}
