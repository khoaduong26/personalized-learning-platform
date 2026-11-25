package com.tlcn.backend.mastery.service.impl;

import com.tlcn.backend.assessment.entity.AssessmentAttempt;
import com.tlcn.backend.assessment.entity.Question;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import com.tlcn.backend.assessment.repository.StudentAnswerRepository;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.mastery.entity.GapPriority;
import com.tlcn.backend.mastery.entity.GapStatus;
import com.tlcn.backend.mastery.entity.KnowledgeGap;
import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import com.tlcn.backend.mastery.entity.MasteryHistory;
import com.tlcn.backend.mastery.entity.MasteryLevel;
import com.tlcn.backend.mastery.entity.StudentError;
import com.tlcn.backend.mastery.repository.KnowledgeGapRepository;
import com.tlcn.backend.mastery.repository.KnowledgeMasteryRepository;
import com.tlcn.backend.mastery.repository.MasteryHistoryRepository;
import com.tlcn.backend.mastery.repository.StudentErrorRepository;
import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.learningpath.entity.LearningPath;
import com.tlcn.backend.learningpath.entity.LearningPathStatus;
import com.tlcn.backend.learningpath.entity.LearningStep;
import com.tlcn.backend.learningpath.entity.LearningStepStatus;
import com.tlcn.backend.learningpath.repository.LearningStepRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.repository.StudentRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class MasteryServiceImpl implements MasteryService {
    private final KnowledgeMasteryRepository mastery;
    private final MasteryHistoryRepository history;
    private final StudentErrorRepository errors;
    private final KnowledgeGapRepository gaps;
    private final StudentAnswerRepository answers;
    private final StudentRepository students;
    private final ClassroomRepository classrooms;
    private final LearningStepRepository learningSteps;

    public MasteryServiceImpl(KnowledgeMasteryRepository mastery, MasteryHistoryRepository history,
            StudentErrorRepository errors, KnowledgeGapRepository gaps, StudentAnswerRepository answers,
            StudentRepository students, ClassroomRepository classrooms, LearningStepRepository learningSteps) {
        this.mastery = mastery;
        this.history = history;
        this.errors = errors;
        this.gaps = gaps;
        this.answers = answers;
        this.students = students;
        this.classrooms = classrooms;
        this.learningSteps = learningSteps;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeMastery> profile(Long studentId) {
        List<KnowledgeMastery> records = mastery.findByStudentId(studentId);
        records.forEach(record -> {
            record.getLesson().getId();
            record.getLesson().getName();
        });
        return records;
    }

    @Transactional(readOnly = true)
    public List<MasteryHistory> history(Long masteryId) {
        return history.findByMasteryIdOrderByRecordedAtAsc(masteryId);
    }

    @Transactional(readOnly = true)
    public List<StudentError> errors(Long studentId) {
        List<StudentError> records = errors.findByStudentIdOrderByCreatedAtDesc(studentId);
        records.forEach(record -> {
            record.getLesson().getId();
            record.getLesson().getName();
        });
        return records;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeGap> gaps(Long studentId) {
        List<KnowledgeGap> records = gaps.findByStudentId(studentId);
        records.forEach(record -> {
            record.getLesson().getId();
            record.getLesson().getName();
        });
        return records;
    }

    @Transactional
    public KnowledgeMastery save(KnowledgeMastery value) {
        return mastery.save(value);
    }

    @Transactional
    public StudentError save(StudentError value) {
        return errors.save(value);
    }

    @Transactional
    public KnowledgeGap save(KnowledgeGap value) {
        return gaps.save(value);
    }

    @Transactional
    public void analyze(AssessmentAttempt attempt) {
        if (attempt.getAssessment() == null && attempt.getLearningStep() == null)
            return;
        List<StudentAnswer> submittedAnswers = answers.findByAttemptId(attempt.getId());
        var answersByQuestion = submittedAnswers.stream().collect(java.util.stream.Collectors.toMap(
                answer -> answer.getQuestion().getId(), answer -> answer));
        List<Question> sourceQuestions = attempt.getAssessment() != null
                ? attempt.getAssessment().getQuestions()
                : attempt.getLearningStep().getExercises().stream()
                        .map(exercise -> exercise.getQuestion())
                        .filter(question -> question.getApprovalStatus() == com.tlcn.backend.assessment.entity.ApprovalStatus.APPROVED)
                        .toList();
        var byLesson = sourceQuestions.stream()
                .collect(java.util.stream.Collectors.groupingBy(Question::getLesson));
        byLesson.forEach((lesson, lessonQuestions) -> {
            String triggerSource = attempt.getAssessment() != null
                    ? "ASSESSMENT_" + attempt.getId()
                    : "LEARNING_STEP_" + attempt.getLearningStep().getId() + "_ATTEMPT_" + attempt.getId();
            int correct = (int) lessonQuestions.stream().map(question -> answersByQuestion.get(question.getId()))
                    .filter(java.util.Objects::nonNull).filter(StudentAnswer::isCorrect).count();
            double attemptScore = 100.0 * correct / lessonQuestions.size();
            Long studentId = attempt.getStudent().getId();
            KnowledgeMastery record = mastery.findByStudentIdAndLessonId(studentId, lesson.getId())
                    .orElseGet(() -> {
                        KnowledgeMastery created = new KnowledgeMastery();
                        created.setStudent(attempt.getStudent());
                        created.setLesson(lesson);
                        return created;
                    });
            if (record.getId() != null && history.existsByMasteryIdAndTriggerSource(record.getId(), triggerSource))
                return;
            boolean existed = record.getId() != null;
            double oldScore = record.getMasteryScore();
            double newScore = existed ? (oldScore + attemptScore) / 2.0 : attemptScore;
            record.setMasteryScore(newScore);
            record.setMasteryLevel(level(newScore));
            long previousEvidence = existed ? history.countByMasteryId(record.getId()) : 0;
            record.setConfidenceScore(Math.min(1.0, (previousEvidence + 1) / 5.0));
            record.setLastUpdated(Instant.now());
            record = mastery.save(record);

            MasteryHistory change = new MasteryHistory();
            change.setMastery(record);
            change.setOldScore(existed ? oldScore : null);
            change.setNewScore(newScore);
            change.setTriggerSource(triggerSource);
            history.save(change);

            for (StudentAnswer answer : submittedAnswers.stream()
                    .filter(value -> value.getQuestion().getLesson().getId().equals(lesson.getId())).toList()) {
                if (!answer.isCorrect() && !errors.existsByAnswerId(answer.getId())) {
                    StudentError error = new StudentError();
                    error.setStudent(attempt.getStudent());
                    error.setLesson(lesson);
                    error.setAnswer(answer);
                    error.setErrorType("Câu trả lời chưa chính xác");
                    error.setDescription(
                            "Câu trả lời chưa đạt đáp án chuẩn. Cần giáo viên xem lại cách làm để xác định nguyên nhân cụ thể.");
                    errors.save(error);
                }
            }

            int recentErrors = lessonQuestions.size() - correct;
            java.util.Optional<KnowledgeGap> existing = gaps.findByStudentIdAndLessonId(studentId, lesson.getId());
            if (newScore < 70 || recentErrors > 0) {
                KnowledgeGap gap = existing.orElseGet(KnowledgeGap::new);
                gap.setStudent(attempt.getStudent());
                gap.setLesson(lesson);
                gap.setPriority(newScore < 40 || recentErrors >= 2 ? GapPriority.HIGH
                        : newScore < 70 ? GapPriority.MEDIUM : GapPriority.LOW);
                gap.setRootCause("Điểm thành thạo hiện tại " + Math.round(newScore)
                        + "/100; có " + recentErrors + " câu sai trong lượt làm này.");
                gap.setStatus(attempt.getLearningStep() == null ? GapStatus.IDENTIFIED : GapStatus.IN_REMEDIATION);
                gaps.save(gap);
            } else if (existing.isPresent()) {
                existing.get().setStatus(GapStatus.RESOLVED);
                existing.get().setRootCause("Đã đạt ngưỡng thành thạo từ 70/100 trở lên.");
            }
        });
        if (attempt.getLearningStep() != null) {
            LearningStep step = attempt.getLearningStep();
            Question lessonQuestion = sourceQuestions.stream()
                    .filter(question -> question.getLesson().getId().equals(step.getLesson().getId()))
                    .findFirst().orElse(null);
            if (lessonQuestion != null) {
                int correct = (int) sourceQuestions.stream()
                        .filter(question -> question.getLesson().getId().equals(step.getLesson().getId()))
                        .map(question -> answersByQuestion.get(question.getId()))
                        .filter(java.util.Objects::nonNull).filter(StudentAnswer::isCorrect).count();
                long total = sourceQuestions.stream()
                        .filter(question -> question.getLesson().getId().equals(step.getLesson().getId())).count();
                double stepScore = total == 0 ? 0 : 100.0 * correct / total;
                updatePathProgress(step, stepScore);
            }
        }
    }

    private void updatePathProgress(LearningStep step, double score) {
        LearningPath path = step.getPath();
        List<LearningStep> orderedSteps = learningSteps.findByPathIdOrderByStepOrderAsc(path.getId());
        boolean changed = false;
        if (score >= (step.getTargetScore() == null ? 70.0 : step.getTargetScore())) {
            if (step.getStatus() != LearningStepStatus.COMPLETED) {
                step.setStatus(LearningStepStatus.COMPLETED);
                changed = true;
            }
            LearningStep nextStep = orderedSteps.stream().filter(next -> next.getStepOrder() > step.getStepOrder())
                    .filter(next -> next.getStatus() == LearningStepStatus.LOCKED)
                    .min(java.util.Comparator.comparing(LearningStep::getStepOrder))
                    .orElse(null);
            if (nextStep != null) {
                nextStep.setStatus(LearningStepStatus.IN_PROGRESS);
                changed = true;
            }
            boolean complete = orderedSteps.stream().allMatch(item -> item.getStatus() == LearningStepStatus.COMPLETED);
            LearningPathStatus nextStatus = complete ? LearningPathStatus.COMPLETED : LearningPathStatus.ACTIVE;
            if (path.getStatus() != nextStatus) {
                path.setStatus(nextStatus);
                changed = true;
            }
        } else {
            if (step.getStatus() != LearningStepStatus.IN_PROGRESS) {
                step.setStatus(LearningStepStatus.IN_PROGRESS);
                changed = true;
            }
            if (path.getStatus() != LearningPathStatus.ACTIVE) {
                path.setStatus(LearningPathStatus.ACTIVE);
                changed = true;
            }
        }
        if (changed) {
            path.setVersion(path.getVersion() + 1);
            path.setUpdatedAt(Instant.now());
        }
        learningSteps.save(step);
    }

    @Transactional(readOnly = true)
    public void requireCanView(Long studentId, UserSummary actor) {
        if (actor == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (actor.role() == Role.ADMIN)
            return;
        var student = students.findById(studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (actor.role() == Role.STUDENT && actor.id().equals(studentId))
            return;
        if (actor.role() == Role.TEACHER && student.getClassroom() != null
                && student.getClassroom().getTeacher().getId().equals(actor.id()))
            return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hồ sơ năng lực này.");
    }

    @Transactional(readOnly = true)
    public void requireCanViewMastery(Long masteryId, UserSummary actor) {
        KnowledgeMastery record = mastery.findById(masteryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireCanView(record.getStudent().getId(), actor);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeMastery> classroomDashboard(Long classroomId, UserSummary actor) {
        if (actor == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var classroom = classrooms.findById(classroomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (actor.role() != Role.ADMIN && (actor.role() != Role.TEACHER
                || !classroom.getTeacher().getId().equals(actor.id())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem lớp học này.");
        List<KnowledgeMastery> records = mastery.findByStudentClassroomId(classroomId);
        records.forEach(record -> {
            record.getLesson().getId();
            record.getLesson().getName();
        });
        return records;
    }

    private MasteryLevel level(double score) {
        if (score >= 90)
            return MasteryLevel.MASTERED;
        if (score >= 75)
            return MasteryLevel.PROFICIENT;
        if (score >= 50)
            return MasteryLevel.DEVELOPING;
        if (score > 0)
            return MasteryLevel.BEGINNING;
        return MasteryLevel.NOT_STARTED;
    }
}
