package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import java.util.List;

public interface SubjectService {
    List<SubjectResponse> findAll();
    SubjectResponse get(Long id);
    SubjectResponse create(SubjectRequest request);
    SubjectResponse update(Long id, SubjectRequest request);
    void delete(Long id);
}
