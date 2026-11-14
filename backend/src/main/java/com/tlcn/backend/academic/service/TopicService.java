package com.tlcn.backend.academic.service;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicService {
    private final TopicRepository topics;
    private final SubjectRepository subjects;
    private final LessonRepository lessons;

    public TopicService(TopicRepository topics, SubjectRepository subjects, LessonRepository lessons) {
        this.topics = topics;
        this.subjects = subjects;
        this.lessons = lessons;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> bySubject(Long subjectId) {
        requireSubject(subjectId);
        return topics.findBySubjectIdOrderByNameAsc(subjectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicResponse get(Long topicId) {
        return toResponse(requireTopic(topicId));
    }

    @Transactional
    public TopicResponse create(TopicRequest request) {
        Subject subject = requireSubject(request.subjectId());
        String name = normalizeName(request.name());
        ensureNameAvailable(subject.getId(), name, null);

        Topic topic = topics.save(new Topic(name, subject));
        return toResponse(topic);
    }

    @Transactional
    public TopicResponse update(Long topicId, TopicRequest request) {
        Topic topic = requireTopic(topicId);
        Subject subject = requireSubject(request.subjectId());
        String name = normalizeName(request.name());
        ensureNameAvailable(subject.getId(), name, topicId);

        topic.setName(name);
        topic.setSubject(subject);
        return toResponse(topics.save(topic));
    }

    @Transactional
    public void delete(Long topicId) {
        Topic topic = requireTopic(topicId);
        if (lessons.existsByTopicId(topicId)) {
            throw new AcademicConflictException("Hãy xóa các bài học trước khi xóa chủ đề.");
        }
        topics.delete(topic);
    }

    private TopicResponse toResponse(Topic topic) {
        Subject subject = topic.getSubject();
        int lessonCount = Math.toIntExact(lessons.countByTopicId(topic.getId()));
        return new TopicResponse(topic.getId(), topic.getName(), subject.getId(), subject.getName(), lessonCount);
    }

    private Subject requireSubject(Long subjectId) {
        return subjects.findById(subjectId)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy môn học."));
    }

    private Topic requireTopic(Long topicId) {
        return topics.findById(topicId)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy chủ đề."));
    }

    private void ensureNameAvailable(Long subjectId, String name, Long excludedTopicId) {
        boolean duplicate = excludedTopicId == null
                ? topics.existsBySubjectIdAndNameIgnoreCase(subjectId, name)
                : topics.existsBySubjectIdAndNameIgnoreCaseAndIdNot(subjectId, name, excludedTopicId);
        if (duplicate) {
            throw new AcademicConflictException("Chủ đề này đã tồn tại trong môn học.");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên chủ đề không được để trống.");
        }
        return name.strip();
    }
}
