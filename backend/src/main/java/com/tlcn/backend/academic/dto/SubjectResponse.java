package com.tlcn.backend.academic.dto;
import com.tlcn.backend.academic.entity.Subject;
public record SubjectResponse(Long id, String name, int topicCount) {
    public static SubjectResponse from(Subject subject) { return new SubjectResponse(subject.getId(), subject.getName(), subject.getTopics().size()); }
}
