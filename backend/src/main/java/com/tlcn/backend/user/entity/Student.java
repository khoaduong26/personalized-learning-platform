package com.tlcn.backend.user.entity;

import com.tlcn.backend.classroom.entity.Classroom;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student extends User {
    private String sex;
    private LocalDate dateOfBirth;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private Classroom classroom;
    // Keep classroom_id as the legacy primary membership. Additional memberships
    // are stored in the join table so existing installations retain their data.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "classroom_students", joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "classroom_id"))
    private Set<Classroom> additionalClassrooms = new LinkedHashSet<>();
    public Student() { setRole(Role.STUDENT); }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }
    public Set<Classroom> getAdditionalClassrooms() { return additionalClassrooms; }
    public Set<Classroom> getClassrooms() {
        Set<Classroom> memberships = new LinkedHashSet<>(additionalClassrooms);
        if (classroom != null) memberships.add(classroom);
        return memberships;
    }
    public boolean belongsToClassroom(Long classroomId) {
        return classroom != null && classroom.getId().equals(classroomId)
                || additionalClassrooms.stream().anyMatch(item -> item.getId().equals(classroomId));
    }
    public void addClassroom(Classroom value) {
        if (classroom == null) classroom = value;
        else if (!classroom.getId().equals(value.getId())) additionalClassrooms.add(value);
    }
    public void removeClassroom(Long classroomId) {
        additionalClassrooms.removeIf(item -> item.getId().equals(classroomId));
        if (classroom != null && classroom.getId().equals(classroomId)) {
            classroom = additionalClassrooms.stream().findFirst().orElse(null);
            if (classroom != null) additionalClassrooms.remove(classroom);
        }
    }
}
