package com.tlcn.backend.learningpath.service.impl;

import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.generation.LearningPathOrderer;
import com.tlcn.backend.learningpath.repository.LearningPathRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.learningpath.service.LearningPathService;
import com.tlcn.backend.notification.NotificationService;
import com.tlcn.backend.mastery.entity.GapPriority;
import com.tlcn.backend.mastery.entity.GapStatus;
import com.tlcn.backend.mastery.entity.KnowledgeGap;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.repository.StudentRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    private static final double TARGET_SCORE = 70.0;
    private static final Logger log = LoggerFactory.getLogger(LearningPathServiceImpl.class);

    private final LearningPathRepository paths;
    private final LearningStepRepository steps;
    private final KnowledgeGapRepository gaps;
    private final StudentRepository students;
    private final LearningPathOrderer orderer;
    private final NotificationService notifications;

    public LearningPathServiceImpl(LearningPathRepository paths, LearningStepRepository steps,
            KnowledgeGapRepository gaps, StudentRepository students, LearningPathOrderer orderer,
            NotificationService notifications) {
        this.paths = paths;
        this.steps = steps;
        this.gaps = gaps;
        this.students = students;
        this.orderer = orderer;
        this.notifications = notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPath> forStudent(Long studentId, UserSummary actor) {
        requireCanView(studentId, actor);
        return paths.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPath get(Long studentId, Long pathId, UserSummary actor) {
        requireCanView(studentId, actor);
        return paths.findByIdAndStudentId(pathId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lộ trình."));
    }

    @Override
    @Transactional
    public LearningPath generate(Long studentId, UserSummary actor) {
        requireCanManage(studentId, actor);
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh."));
        List<KnowledgeGap> openGaps = gaps.findByStudentId(studentId).stream()
                .filter(gap -> gap.getStatus() == GapStatus.IDENTIFIED || gap.getStatus() == GapStatus.IN_REMEDIATION)
                .sorted(Comparator.comparingInt((KnowledgeGap gap) -> priorityOrder(gap.getPriority()))
                        .thenComparing(KnowledgeGap::getIdentifiedAt)
                        .thenComparing(gap -> gap.getLesson().getId()))
                .toList();
        List<LearningPath> history = paths.findByStudentIdOrderByCreatedAtDesc(studentId);
        LearningPath activePath = history.stream()
                .filter(path -> path.getStatus() == LearningPathStatus.ACTIVE)
                .findFirst()
                .orElse(null);
        if (openGaps.isEmpty()) {
            if (activePath != null) {
                return activePath;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Học sinh hiện không có khoảng trống kiến thức cần đưa vào lộ trình.");
        }
        if (activePath != null && matchesCurrentGaps(activePath, openGaps)) {
            return activePath;
        }

        List<LearningPathOrderer.Candidate> candidates = java.util.stream.IntStream.range(0, openGaps.size())
                .mapToObj(position -> {
                    KnowledgeGap gap = openGaps.get(position);
                    return new LearningPathOrderer.Candidate(position, gap.getLesson().getName(),
                            gap.getLesson().getDescription(), gap.getPriority().name());
                }).toList();
        List<KnowledgeGap> orderedGaps = openGaps;
        try {
            List<Integer> positions = orderer.order(candidates);
            List<KnowledgeGap> suggestedOrder = validateSuggestion(positions, openGaps);
            if (suggestedOrder == null) {
                log.warn("Learning path order suggestion from {} was invalid; using backend priority order.",
                        orderer.getClass().getSimpleName());
            } else if (!preservesPriority(suggestedOrder)) {
                log.warn("Learning path order suggestion from {} violated backend priority rules; using backend priority order.",
                        orderer.getClass().getSimpleName());
            } else {
                orderedGaps = suggestedOrder;
            }
        } catch (RuntimeException exception) {
            log.warn("Learning path order suggestion from {} failed; using backend priority order.",
                    orderer.getClass().getSimpleName());
        }

        history.stream()
                .filter(path -> path.getStatus() == LearningPathStatus.ACTIVE)
                .forEach(path -> {
                    path.setStatus(LearningPathStatus.ARCHIVED);
                    path.setUpdatedAt(Instant.now());
                });

        LearningPath path = new LearningPath();
        path.setStudent(student);
        path.setVersion(paths.findMaxVersionByStudentId(studentId) + 1);
        path.setUpdatedAt(Instant.now());

        for (int index = 0; index < orderedGaps.size(); index++) {
            KnowledgeGap gap = orderedGaps.get(index);
            LearningStep step = new LearningStep();
            step.setStepOrder(index + 1);
            step.setStatus(index == 0 ? LearningStepStatus.IN_PROGRESS : LearningStepStatus.LOCKED);
            step.setTargetScore(TARGET_SCORE);
            step.setPath(path);
            step.setLesson(gap.getLesson());
            path.getSteps().add(step);
            gap.setStatus(GapStatus.IN_REMEDIATION);
        }
        LearningPath saved = paths.save(path);
        notifications.notifyAfterCommit(actor.id(), "LEARNING_PATH_READY", "Lộ trình đã sẵn sàng",
                "Đã tạo lộ trình học tập cho " + student.getFullName() + " từ các khoảng trống kiến thức.",
                "/classrooms");
        notifications.notifyAfterCommit(student.getId(), "LEARNING_PATH_READY", "Lộ trình học tập mới",
                "Thầy cô đã tạo lộ trình học tập cá nhân cho em.", "/my-learning-path");
        return saved;
    }

    private boolean matchesCurrentGaps(LearningPath activePath, List<KnowledgeGap> openGaps) {
        if (activePath.getSteps().size() != openGaps.size()) {
            return false;
        }

        List<Long> activeLessonIds = activePath.getSteps().stream()
                .map(step -> step.getLesson().getId())
                .sorted()
                .toList();
        List<Long> currentLessonIds = openGaps.stream()
                .map(gap -> gap.getLesson().getId())
                .sorted()
                .toList();
        if (!activeLessonIds.equals(currentLessonIds)) {
            return false;
        }

        Map<Long, Integer> currentPriorities = new java.util.HashMap<>();
        for (KnowledgeGap gap : openGaps) {
            currentPriorities.put(gap.getLesson().getId(), priorityOrder(gap.getPriority()));
        }
        int previousPriority = -1;
        for (LearningStep step : activePath.getSteps()) {
            Integer priority = currentPriorities.get(step.getLesson().getId());
            if (priority == null || priority < previousPriority) {
                return false;
            }
            previousPriority = priority;
        }
        return true;
    }

    private List<KnowledgeGap> validateSuggestion(List<Integer> positions, List<KnowledgeGap> backendOrder) {
        if (positions == null || positions.size() != backendOrder.size()) {
            return null;
        }
        Set<Integer> seenPositions = new HashSet<>();
        for (Integer position : positions) {
            if (position == null || position < 0 || position >= backendOrder.size() || !seenPositions.add(position)) {
                return null;
            }
        }
        return positions.stream().map(backendOrder::get).toList();
    }

    private boolean preservesPriority(List<KnowledgeGap> suggestion) {
        int previousPriority = -1;
        for (KnowledgeGap gap : suggestion) {
            int currentPriority = priorityOrder(gap.getPriority());
            if (currentPriority < previousPriority) {
                return false;
            }
            previousPriority = currentPriority;
        }
        return true;
    }

    @Override
    @Transactional
    public LearningPath updateStepStatus(Long studentId, Long pathId, Long stepId, LearningStepStatus status,
            UserSummary actor) {
        requireCanView(studentId, actor);
        if (actor.role() != Role.STUDENT || !actor.id().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ học sinh mới có thể cập nhật tiến độ của mình.");
        }
        LearningPath path = paths.findByIdAndStudentId(pathId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lộ trình."));
        if (path.getStatus() != LearningPathStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lộ trình này không còn hoạt động.");
        }
        LearningStep step = steps.findByIdAndPathId(stepId, pathId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bước học."));
        if (status == LearningStepStatus.LOCKED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể chuyển một bước học về trạng thái khóa.");
        }
        if (status == LearningStepStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Chỉ hoàn thành bước học sau khi nộp bài luyện tập và đạt điểm mục tiêu.");
        }
        if (status == LearningStepStatus.IN_PROGRESS) {
            if (step.getStatus() == LearningStepStatus.IN_PROGRESS) {
                return path;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bước học này chưa được mở hoặc đã hoàn thành.");
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Chỉ có thể tiếp tục bước học đang mở.");
    }

    private void requireCanManage(Long studentId, UserSummary actor) {
        requireCanView(studentId, actor);
        if (actor.role() == Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Học sinh không thể tạo lộ trình.");
        }
    }

    private void requireCanView(Long studentId, UserSummary actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập.");
        }
        if (actor.role() == Role.ADMIN) {
            return;
        }
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy học sinh."));
        if (actor.role() == Role.STUDENT && actor.id().equals(studentId)) {
            return;
        }
        if (actor.role() == Role.TEACHER && student.getClassroom() != null
                && student.getClassroom().getTeacher().getId().equals(actor.id())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem lộ trình này.");
    }

    private int priorityOrder(GapPriority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }
}
