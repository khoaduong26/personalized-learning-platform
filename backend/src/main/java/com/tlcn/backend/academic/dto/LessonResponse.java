package com.tlcn.backend.academic.dto;

import java.util.List;

public record LessonResponse(Long id, String name, String description, String learningContent,
        List<LessonImageRequest> images,
        Long topicId, String topicName, Long subjectId, String subjectName) {
}
