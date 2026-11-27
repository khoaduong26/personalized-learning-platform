package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import com.tlcn.backend.user.dto.UserSummary;
import java.util.List;

public interface TopicService {
    List<TopicResponse> bySubject(Long subjectId, UserSummary actor);
    TopicResponse get(Long topicId, UserSummary actor);
    TopicResponse create(TopicRequest request, UserSummary actor);
    TopicResponse update(Long topicId, TopicRequest request, UserSummary actor);
    void delete(Long topicId, UserSummary actor);
}
