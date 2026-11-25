package com.tlcn.backend.mastery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.mastery.controller.MasteryDashboardController;
import com.tlcn.backend.mastery.entity.KnowledgeMastery;
import com.tlcn.backend.mastery.service.MasteryService;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

class MasteryDashboardControllerTests {
    private final MasteryService service = mock(MasteryService.class);
    private final MasteryDashboardController controller = new MasteryDashboardController(service);

    @Test
    void dashboardGroupsByLessonAndCountsScoresBelowSeventyForReview() {
        Lesson lesson = mock(Lesson.class);
        when(lesson.getId()).thenReturn(22L);
        when(lesson.getName()).thenReturn("Cộng có nhớ trong phạm vi 100");
        when(service.classroomDashboard(15L,
                new UserSummary(3L, "Cô Lan", "teacher@example.invalid", Role.TEACHER)))
                .thenReturn(List.of(mastery(lesson, 55), mastery(lesson, 85)));

        var result = controller.dashboard(15L,
                new UserSummary(3L, "Cô Lan", "teacher@example.invalid", Role.TEACHER));

        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).lessonId());
        assertEquals("Cộng có nhớ trong phạm vi 100", result.get(0).lessonName());
        assertEquals(70.0, result.get(0).averageMastery());
        assertEquals(2L, result.get(0).studentsTracked());
        assertEquals(1L, result.get(0).studentsNeedingReview());
    }

    private KnowledgeMastery mastery(Lesson lesson, double score) {
        KnowledgeMastery record = new KnowledgeMastery();
        record.setLesson(lesson);
        record.setMasteryScore(score);
        return record;
    }
}
