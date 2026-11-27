package com.tlcn.backend.academic.service.impl;

import com.tlcn.backend.academic.dto.SubjectRequest;
import com.tlcn.backend.academic.dto.SubjectResponse;
import com.tlcn.backend.academic.entity.Subject;
import com.tlcn.backend.academic.entity.Topic;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.academic.repository.LessonRepository;
import com.tlcn.backend.academic.repository.SubjectRepository;
import com.tlcn.backend.academic.repository.TopicRepository;
import com.tlcn.backend.academic.service.AcademicConflictException;
import com.tlcn.backend.academic.service.AcademicBadRequestException;
import com.tlcn.backend.academic.service.AcademicNotFoundException;
import com.tlcn.backend.academic.service.SubjectService;
import com.tlcn.backend.academic.service.TopicService;
import com.tlcn.backend.assessment.entity.Blank;
import com.tlcn.backend.assessment.entity.FillBlankQuestion;
import com.tlcn.backend.assessment.entity.MultipleChoiceQuestion;
import com.tlcn.backend.assessment.entity.Option;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.entity.ShortAnswerQuestion;
import com.tlcn.backend.assessment.repository.QuestionRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository repository;
    private final TopicRepository topics;
    private final LessonRepository lessons;
    private final UserRepository users;
    private final TopicService topicService;
    private final QuestionRepository questions;

    public SubjectServiceImpl(SubjectRepository repository, TopicRepository topics,
            LessonRepository lessons, UserRepository users, TopicService topicService, QuestionRepository questions) {
        this.repository = repository;
        this.topics = topics;
        this.lessons = lessons;
        this.users = users;
        this.topicService = topicService;
        this.questions = questions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> findAll(UserSummary actor) {
        requireSignedIn(actor);
        List<Subject> visible = actor.role() == Role.ADMIN
                ? repository.findAllByOrderByNameAsc()
                : actor.role() == Role.STUDENT
                    ? repository.findByOwnerIsNullOrderByNameAsc()
                    : repository.findVisibleForOwner(actor.id());
        return visible.stream().map(this::response).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse get(Long id, UserSummary actor) {
        requireSignedIn(actor);
        return response(requireVisibleSubject(id, actor));
    }

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request, UserSummary actor) {
        requireTeacherOrAdmin(actor);
        String name = normalizeName(request.name());
        User owner = actor.role() == Role.ADMIN ? null : requireUser(actor.id());
        ensureNameAvailable(name, owner == null ? null : owner.getId(), null);
        return response(repository.save(new Subject(name, owner)));
    }

    @Override
    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request, UserSummary actor) {
        Subject subject = requireEditableSubject(id, actor);
        String name = normalizeName(request.name());
        ensureNameAvailable(name, subject.getOwner() == null ? null : subject.getOwner().getId(), subject.getId());
        subject.setName(name);
        return response(repository.save(subject));
    }

    @Override
    @Transactional
    public void delete(Long id, UserSummary actor) {
        Subject subject = requireEditableSubject(id, actor);
        topics.findBySubjectIdOrderByNameAsc(id)
                .forEach(topic -> topicService.delete(topic.getId(), actor));
        repository.delete(subject);
    }

    @Override
    @Transactional
    public SubjectResponse copy(Long id, UserSummary actor) {
        requireTeacher(actor);
        Subject source = requireVisibleSubject(id, actor);
        if (source.getOwner() != null) {
            throw new AcademicConflictException("Chỉ có thể lưu bản sao từ chương trình chung.");
        }
        User owner = requireUser(actor.id());
        if (repository.existsByNameIgnoreCaseAndOwner_Id(source.getName(), owner.getId())) {
            throw new AcademicConflictException("Bạn đã có bản riêng của môn học này.");
        }

        Subject copy = repository.save(new Subject(source.getName(), owner));
        for (Topic sourceTopic : topics.findBySubjectIdOrderByNameAsc(source.getId())) {
            Topic topicCopy = topics.save(new Topic(sourceTopic.getName(), copy));
            for (Lesson sourceLesson : lessons.findByTopicIdOrderByNameAsc(sourceTopic.getId())) {
                Lesson lessonCopy = lessons.save(new Lesson(sourceLesson.getName(), sourceLesson.getDescription(), topicCopy));
                for (Question sourceQuestion : questions.findByLessonId(sourceLesson.getId())) {
                    questions.save(copyQuestion(sourceQuestion, lessonCopy));
                }
            }
        }
        return response(copy);
    }

    private Question copyQuestion(Question source, Lesson targetLesson) {
        source = (Question) Hibernate.unproxy(source);
        Question copy;
        if (source instanceof MultipleChoiceQuestion original) {
            MultipleChoiceQuestion multipleChoice = new MultipleChoiceQuestion();
            multipleChoice.setAllowMultiple(original.isAllowMultiple());
            multipleChoice.setOptions(original.getOptions().stream().map(option -> {
                Option optionCopy = new Option();
                optionCopy.setName(option.getName());
                optionCopy.setCorrect(option.isCorrect());
                optionCopy.setDisplayOrder(option.getDisplayOrder());
                return optionCopy;
            }).toList());
            copy = multipleChoice;
        } else if (source instanceof FillBlankQuestion original) {
            FillBlankQuestion fillBlank = new FillBlankQuestion();
            fillBlank.setCaseSensitive(original.isCaseSensitive());
            fillBlank.setBlanks(original.getBlanks().stream().map(blank -> {
                Blank blankCopy = new Blank();
                blankCopy.setCorrectAnswer(blank.getCorrectAnswer());
                blankCopy.setPosition(blank.getPosition());
                return blankCopy;
            }).toList());
            copy = fillBlank;
        } else if (source instanceof ShortAnswerQuestion original) {
            ShortAnswerQuestion shortAnswer = new ShortAnswerQuestion();
            shortAnswer.setCaseSensitive(original.isCaseSensitive());
            shortAnswer.setCorrectAnswer(original.getCorrectAnswer());
            copy = shortAnswer;
        } else {
            throw new AcademicConflictException("Có dạng câu hỏi không thể sao chép trong chương trình này.");
        }
        copy.setContent(source.getContent());
        copy.setDifficulty(source.getDifficulty());
        copy.setScore(source.getScore());
        copy.setLesson(targetLesson);
        copy.setAiGenerated(source.isAiGenerated());
        copy.setApprovalStatus(source.getApprovalStatus());
        copy.setExplanation(source.getExplanation());
        return copy;
    }

    private SubjectResponse response(Subject subject) {
        Long ownerId = subject.getOwner() == null ? null : subject.getOwner().getId();
        return new SubjectResponse(subject.getId(), subject.getName(),
                (int) topics.countBySubjectId(subject.getId()), ownerId, ownerId == null);
    }

    private Subject requireVisibleSubject(Long id, UserSummary actor) {
        Subject subject = repository.findById(id)
                .orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy môn học."));
        if (actor.role() != Role.ADMIN && subject.getOwner() != null
                && !subject.getOwner().getId().equals(actor.id())) {
            throw new AcademicNotFoundException("Không tìm thấy môn học.");
        }
        return subject;
    }

    private Subject requireEditableSubject(Long id, UserSummary actor) {
        requireTeacherOrAdmin(actor);
        Subject subject = requireVisibleSubject(id, actor);
        if (subject.getOwner() == null && actor.role() != Role.ADMIN) {
            throw new AcademicConflictException("Chương trình chung chỉ đọc. Hãy lưu về bản riêng để chỉnh sửa.");
        }
        return subject;
    }

    private void ensureNameAvailable(String name, Long ownerId, Long excludedId) {
        boolean duplicate = ownerId == null
                ? repository.existsByNameIgnoreCaseAndOwnerIsNull(name)
                : repository.existsByNameIgnoreCaseAndOwner_Id(name, ownerId);
        if (duplicate && (excludedId == null || !repository.findById(excludedId)
                .map(subject -> subject.getName().equalsIgnoreCase(name)).orElse(false))) {
            throw new AcademicConflictException("Tên môn học đã tồn tại trong danh sách của bạn.");
        }
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new AcademicBadRequestException("Tên môn học không được để trống.");
        }
        return name.strip();
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new AcademicNotFoundException("Không tìm thấy tài khoản."));
    }

    private void requireTeacher(UserSummary actor) {
        if (actor == null || actor.role() != Role.TEACHER) {
            throw new AcademicNotFoundException("Không tìm thấy chương trình học.");
        }
    }

    private void requireTeacherOrAdmin(UserSummary actor) {
        if (actor == null || (actor.role() != Role.TEACHER && actor.role() != Role.ADMIN)) {
            throw new AcademicNotFoundException("Không tìm thấy chương trình học.");
        }
    }

    private void requireSignedIn(UserSummary actor) {
        if (actor == null) throw new AcademicNotFoundException("Không tìm thấy chương trình học.");
    }
}
