package com.tlcn.backend.user.service;

import com.tlcn.backend.user.entity.Student;
import java.util.List;

public interface StudentService {
    List<Student> findAll();

    Student save(Student student);
}
