package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.entity.Subject;
import java.util.List;

public interface SubjectService {

    List<Subject> findAll();

    Subject save(Subject subject);

}
