package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import com.tlcn.backend.academic.service.LessonService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository repository;
    private final TopicRepository topics;

    public LessonServiceImpl(LessonRepository repository, TopicRepository topics) {
        this.repository = repository;
        this.topics = topics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> byTopic(Long id) {
        requireTopic(id);
        return repository.findByTopicIdOrderByNameAsc(id).stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse get(Long id) {
        return response(requireLesson(id));
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request) {
        var topic = requireTopic(request.topicId());
        String name = request.name().strip();
        ensureUnique(topic.getId(), name, null);
        Lesson lesson = new Lesson(name, trimDescription(request.description()), topic);
        return response(repository.save(lesson));
    }

    @Override
    @Transactional
    public LessonResponse update(Long id, LessonRequest request) {
        Lesson lesson = requireLesson(id);
        var topic = requireTopic(request.topicId());
        String name = request.name().strip();
        ensureUnique(topic.getId(), name, id);
        lesson.setName(name);
        lesson.setDescription(trimDescription(request.description()));
        lesson.setTopic(topic);
        return response(repository.save(lesson));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(requireLesson(id));
    }

    private LessonResponse response(Lesson lesson) {
        var topic = lesson.getTopic();
        return new LessonResponse(lesson.getId(), lesson.getName(), lesson.getDescription(),
                topic.getId(), topic.getName(), topic.getSubject().getId());
    }

    private com.tlcn.backend.academic.entity.Topic requireTopic(Long id) {
        return topics.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy chủ đề."));
    }

    private Lesson requireLesson(Long id) {
        return repository.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy bài học."));
    }

    private void ensureUnique(Long topicId, String name, Long exceptId) {
        if (repository.existsByTopicIdAndNameIgnoreCase(topicId, name)
                && (exceptId == null || !repository.findById(exceptId).map(lesson ->
                        lesson.getTopic().getId().equals(topicId) && lesson.getName().equalsIgnoreCase(name)).orElse(false))) {
            throw new AcademicConflictException("Bài học này đã tồn tại trong chủ đề.");
        }
    }

    private String trimDescription(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
