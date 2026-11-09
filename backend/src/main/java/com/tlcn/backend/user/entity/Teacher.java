package com.tlcn.backend.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "teachers")
public class Teacher extends User {
    protected Teacher() { setRole(Role.TEACHER); }
}
