package com.tlcn.backend.academic.dto;

public record TopicResponse(Long id, String name, Long subjectId, String subjectName, int lessonCount) {}
