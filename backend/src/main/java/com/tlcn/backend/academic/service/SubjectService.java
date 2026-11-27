package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;

public interface SubjectService {
    List<SubjectResponse> findAll(UserSummary actor);
    SubjectResponse get(Long id, UserSummary actor);
    SubjectResponse create(SubjectRequest request, UserSummary actor);
    SubjectResponse update(Long id, SubjectRequest request, UserSummary actor);
    void delete(Long id, UserSummary actor);
    SubjectResponse copy(Long id, UserSummary actor);
}
