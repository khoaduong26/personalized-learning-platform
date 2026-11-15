package com.tlcn.backend.user.service;

import java.util.List;
import com.tlcn.backend.user.entity.Teacher;

public interface TeacherService {

    List<Teacher> findAll();

    Teacher save(Teacher teacher);
}
