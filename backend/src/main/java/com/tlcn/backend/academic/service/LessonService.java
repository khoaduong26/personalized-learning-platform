package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface LessonService {
    List<LessonResponse> byTopic(Long id, UserSummary actor);
    LessonResponse get(Long id, UserSummary actor);
    String uploadImage(Long id, MultipartFile file, UserSummary actor);
    LessonResponse create(LessonRequest request, UserSummary actor);
    LessonResponse update(Long id, LessonRequest request, UserSummary actor);
    void delete(Long id, UserSummary actor);
}
