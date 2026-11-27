package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.dto.LessonRequest;
import com.tlcn.backend.academic.dto.LessonResponse;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.academic.service.AcademicBadRequestException;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import com.tlcn.backend.academic.service.LessonService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {
    private final LessonRepository repository;
    private final TopicRepository topics;
    private final QuestionRepository questions;
    private final LearningStepRepository learningSteps;
    private final KnowledgeMasteryRepository mastery;
    private final KnowledgeGapRepository gaps;
    private final StudentErrorRepository studentErrors;

    public LessonServiceImpl(LessonRepository repository, TopicRepository topics,
            QuestionRepository questions, LearningStepRepository learningSteps,
            KnowledgeMasteryRepository mastery, KnowledgeGapRepository gaps,
            StudentErrorRepository studentErrors) {
        this.repository = repository;
        this.topics = topics;
        this.questions = questions;
        this.learningSteps = learningSteps;
        this.mastery = mastery;
        this.gaps = gaps;
        this.studentErrors = studentErrors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> byTopic(Long id, UserSummary actor) {
        requireTopic(id, actor);
        return repository.findByTopicIdOrderByNameAsc(id).stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonResponse get(Long id, UserSummary actor) {
        return response(requireLesson(id, actor));
    }

    @Override
    @Transactional
    public LessonResponse create(LessonRequest request, UserSummary actor) {
        Topic topic = requireEditableTopic(request.topicId(), actor);
        String name = normalizeName(request.name());
        ensureUnique(topic.getId(), name, null);
        return response(repository.save(new Lesson(name, trimDescription(request.description()), topic)));
    }

    @Override
    @Transactional
    public LessonResponse update(Long id, LessonRequest request, UserSummary actor) {
        Lesson lesson = requireLesson(id, actor);
        requireEditableTopic(lesson.getTopic().getId(), actor);
        Topic topic = requireEditableTopic(request.topicId(), actor);
        String name = normalizeName(request.name());
        ensureUnique(topic.getId(), name, id);
        lesson.setName(name);
        lesson.setDescription(trimDescription(request.description()));
        lesson.setTopic(topic);
        return response(repository.save(lesson));
    }

    @Override
    @Transactional
    public void delete(Long id, UserSummary actor) {
        Lesson lesson = requireEditableLesson(id, actor);
        if (isLinkedToLearningData(id)) {
            throw new AcademicConflictException(
                    "Không thể xóa bài học đã được dùng trong câu hỏi, lộ trình hoặc dữ liệu tiến độ học sinh.");
        }
        repository.delete(lesson);
    }

    private boolean isLinkedToLearningData(Long lessonId) {
        return questions.existsByLessonId(lessonId)
                || learningSteps.existsByLessonId(lessonId)
                || mastery.existsByLessonId(lessonId)
                || gaps.existsByLessonId(lessonId)
                || studentErrors.existsByLessonId(lessonId);
    }

    private LessonResponse response(Lesson lesson) {
        Topic topic = lesson.getTopic();
        return new LessonResponse(lesson.getId(), lesson.getName(), lesson.getDescription(),
                topic.getId(), topic.getName(), topic.getSubject().getId());
    }

    private Topic requireTopic(Long id, UserSummary actor) {
        Topic topic = topics.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy chủ đề."));
        if (!canView(topic.getSubject(), actor)) throw new AcademicNotFoundException("Không tìm thấy chủ đề.");
        return topic;
    }

    private Topic requireEditableTopic(Long id, UserSummary actor) {
        Topic topic = requireTopic(id, actor);
        if (topic.getSubject().getOwner() == null && (actor == null || actor.role() != Role.ADMIN)) {
            throw new AcademicConflictException("Chương trình chung chỉ đọc. Hãy lưu về bản riêng để chỉnh sửa.");
        }
        return topic;
    }

    private Lesson requireLesson(Long id, UserSummary actor) {
        Lesson lesson = repository.findById(id)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy bài học."));
        if (!canView(lesson.getTopic().getSubject(), actor)) {
            throw new AcademicNotFoundException("Không tìm thấy bài học.");
        }
        return lesson;
    }

    private Lesson requireEditableLesson(Long id, UserSummary actor) {
        Lesson lesson = requireLesson(id, actor);
        requireEditableTopic(lesson.getTopic().getId(), actor);
        return lesson;
    }

    private boolean canView(Subject subject, UserSummary actor) {
        return actor != null && (actor.role() == Role.ADMIN || subject.getOwner() == null
                || subject.getOwner().getId().equals(actor.id()));
    }

    private void ensureUnique(Long topicId, String name, Long exceptId) {
        if (repository.existsByTopicIdAndNameIgnoreCase(topicId, name)
                && (exceptId == null || !repository.findById(exceptId).map(
                        lesson -> lesson.getTopic().getId().equals(topicId)
                                && lesson.getName().equalsIgnoreCase(name)).orElse(false))) {
            throw new AcademicConflictException("Bài học này đã tồn tại trong chủ đề.");
        }
    }

    private String normalizeName(String value) {
        if (value == null || value.isBlank()) throw new AcademicBadRequestException("Tên bài học không được để trống.");
        return value.strip();
    }

    private String trimDescription(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
