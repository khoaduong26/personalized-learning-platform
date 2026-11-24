package com.tlcn.backend.learningpath.service.impl;

import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.generation.LearningPathOrderer;
import com.tlcn.backend.learningpath.repository.LearningPathRepository;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.learningpath.service.LearningPathService;
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
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningPathServiceImpl implements LearningPathService {
    private static final double TARGET_SCORE = 70.0;

    private final LearningPathRepository paths;
    private final LearningStepRepository steps;
    private final KnowledgeGapRepository gaps;
    private final StudentRepository students;
    private final LearningPathOrderer orderer;

    public LearningPathServiceImpl(LearningPathRepository paths, LearningStepRepository steps,
            KnowledgeGapRepository gaps, StudentRepository students, LearningPathOrderer orderer) {
        this.paths = paths;
        this.steps = steps;
        this.gaps = gaps;
        this.students = students;
        this.orderer = orderer;
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
        if (openGaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Học sinh hiện không có khoảng trống kiến thức cần đưa vào lộ trình.");
        }

        List<LearningPathOrderer.Candidate> candidates = java.util.stream.IntStream.range(0, openGaps.size())
                .mapToObj(position -> {
                    KnowledgeGap gap = openGaps.get(position);
                    return new LearningPathOrderer.Candidate(position, gap.getLesson().getName(),
                            gap.getLesson().getDescription(), gap.getPriority().name());
                }).toList();
        List<Integer> positions;
        try {
            positions = orderer.order(candidates);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không thể tạo đề xuất thứ tự lộ trình. Chưa có thay đổi nào được lưu.");
        }
        if (positions == null || positions.size() != openGaps.size()) {
            throw invalidOrder();
        }
        Set<Integer> seenPositions = new HashSet<>();
        for (Integer position : positions) {
            if (position == null || position < 0 || position >= openGaps.size() || !seenPositions.add(position)) {
                throw invalidOrder();
            }
        }
        List<KnowledgeGap> orderedGaps = positions.stream().map(openGaps::get).toList();

        paths.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
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
        return paths.save(path);
    }

    private ResponseStatusException invalidOrder() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "AI trả về thứ tự lộ trình không hợp lệ. Chưa có thay đổi nào được lưu.");
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
        if (status == LearningStepStatus.IN_PROGRESS) {
            if (step.getStatus() == LearningStepStatus.IN_PROGRESS) {
                return path;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bước học này chưa được mở hoặc đã hoàn thành.");
        }
        if (step.getStatus() != LearningStepStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hoàn thành bước đang học.");
        }

        step.setStatus(LearningStepStatus.COMPLETED);
        steps.save(step);
        List<LearningStep> orderedSteps = steps.findByPathIdOrderByStepOrderAsc(pathId);
        orderedSteps.stream()
                .filter(next -> next.getStepOrder() > step.getStepOrder())
                .filter(next -> next.getStatus() == LearningStepStatus.LOCKED)
                .findFirst()
                .ifPresent(next -> {
                    next.setStatus(LearningStepStatus.IN_PROGRESS);
                    steps.save(next);
                });
        boolean complete = orderedSteps.stream().allMatch(item -> item.getId().equals(stepId)
                || item.getStatus() == LearningStepStatus.COMPLETED);
        if (complete) {
            path.setStatus(LearningPathStatus.COMPLETED);
        }
        path.setUpdatedAt(Instant.now());
        paths.save(path);
        return paths.findByIdAndStudentId(pathId, studentId).orElse(path);
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
