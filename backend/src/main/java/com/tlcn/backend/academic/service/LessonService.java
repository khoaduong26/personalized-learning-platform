package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import java.util.List;

public interface LessonService {
    List<LessonResponse> byTopic(Long id);
    LessonResponse get(Long id);
    LessonResponse create(LessonRequest request);
    LessonResponse update(Long id, LessonRequest request);
    void delete(Long id);
}
