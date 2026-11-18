package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import java.util.List;

public interface TopicService {
    List<TopicResponse> bySubject(Long subjectId);
    TopicResponse get(Long topicId);
    TopicResponse create(TopicRequest request);
    TopicResponse update(Long topicId, TopicRequest request);
    void delete(Long topicId);
}
