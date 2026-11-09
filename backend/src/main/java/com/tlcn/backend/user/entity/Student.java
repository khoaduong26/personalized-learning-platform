package com.tlcn.backend.user.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "students")
public class Student extends User {
    private String sex;
    private LocalDate dateOfBirth;
    protected Student() { setRole(Role.STUDENT); }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
}
