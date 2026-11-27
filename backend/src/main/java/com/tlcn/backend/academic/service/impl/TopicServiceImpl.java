package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.dto.TopicRequest;
import com.tlcn.backend.academic.dto.TopicResponse;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.service.AcademicBadRequestException;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import com.tlcn.backend.academic.service.LessonService;
import com.tlcn.backend.academic.service.TopicService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicServiceImpl implements TopicService {
    private final TopicRepository topics;
    private final SubjectRepository subjects;
    private final LessonRepository lessons;
    private final LessonService lessonService;

    public TopicServiceImpl(TopicRepository topics, SubjectRepository subjects, LessonRepository lessons,
            LessonService lessonService) {
        this.topics = topics;
        this.subjects = subjects;
        this.lessons = lessons;
        this.lessonService = lessonService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> bySubject(Long subjectId, UserSummary actor) {
        requireSubject(subjectId, actor);
        return topics.findBySubjectIdOrderByNameAsc(subjectId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse get(Long topicId, UserSummary actor) {
        return toResponse(requireTopic(topicId, actor));
    }

    @Override
    @Transactional
    public TopicResponse create(TopicRequest request, UserSummary actor) {
        Subject subject = requireEditableSubject(request.subjectId(), actor);
        String name = normalizeName(request.name());
        ensureNameAvailable(subject.getId(), name, null);
        return toResponse(topics.save(new Topic(name, subject)));
    }

    @Override
    @Transactional
    public TopicResponse update(Long topicId, TopicRequest request, UserSummary actor) {
        Topic topic = requireTopic(topicId, actor);
        Subject subject = requireEditableSubject(request.subjectId(), actor);
        if (!topic.getSubject().getId().equals(subject.getId())) {
            requireEditableSubject(topic.getSubject().getId(), actor);
        }
        String name = normalizeName(request.name());
        ensureNameAvailable(subject.getId(), name, topicId);
        topic.setName(name);
        topic.setSubject(subject);
        return toResponse(topics.save(topic));
    }

    @Override
    @Transactional
    public void delete(Long topicId, UserSummary actor) {
        Topic topic = requireTopic(topicId, actor);
        requireEditableSubject(topic.getSubject().getId(), actor);
        lessons.findByTopicIdOrderByNameAsc(topicId)
                .forEach(lesson -> lessonService.delete(lesson.getId(), actor));
        topics.delete(topic);
    }

    private TopicResponse toResponse(Topic topic) {
        Subject subject = topic.getSubject();
        return new TopicResponse(topic.getId(), topic.getName(), subject.getId(), subject.getName(),
                Math.toIntExact(lessons.countByTopicId(topic.getId())));
    }

    private Subject requireSubject(Long id, UserSummary actor) {
        Subject subject = subjects.findById(id)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy môn học."));
        if (!canView(subject, actor)) throw new AcademicNotFoundException("Không tìm thấy môn học.");
        return subject;
    }

    private Subject requireEditableSubject(Long id, UserSummary actor) {
        Subject subject = requireSubject(id, actor);
        if (subject.getOwner() == null && (actor == null || actor.role() != Role.ADMIN)) {
            throw new AcademicConflictException("Chương trình chung chỉ đọc. Hãy lưu về bản riêng để chỉnh sửa.");
        }
        return subject;
    }

    private Topic requireTopic(Long id, UserSummary actor) {
        Topic topic = topics.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy chủ đề."));
        if (!canView(topic.getSubject(), actor)) throw new AcademicNotFoundException("Không tìm thấy chủ đề.");
        return topic;
    }

    private boolean canView(Subject subject, UserSummary actor) {
        return actor != null && (actor.role() == Role.ADMIN || subject.getOwner() == null
                || subject.getOwner().getId().equals(actor.id()));
    }

    private void ensureNameAvailable(Long subjectId, String name, Long excludedTopicId) {
        boolean duplicate = excludedTopicId == null
                ? topics.existsBySubjectIdAndNameIgnoreCase(subjectId, name)
                : topics.existsBySubjectIdAndNameIgnoreCaseAndIdNot(subjectId, name, excludedTopicId);
        if (duplicate) throw new AcademicConflictException("Chủ đề này đã tồn tại trong môn học.");
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new AcademicBadRequestException("Tên chủ đề không được để trống.");
        return name.strip();
    }
}
