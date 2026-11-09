package com.tlcn.backend.mastery.controller;
import com.tlcn.backend.mastery.entity.MasteryHistory;
import com.tlcn.backend.mastery.service.MasteryService;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/mastery/{masteryId}/history")
public class MasteryHistoryController {
    public record HistoryView(Long id, Double oldScore, Double newScore, Instant recordedAt, String triggerSource) {
        static HistoryView from(MasteryHistory h) { return new HistoryView(h.getId(),h.getOldScore(),h.getNewScore(),h.getRecordedAt(),h.getTriggerSource()); }
    }
    private final MasteryService service;
    public MasteryHistoryController(MasteryService service) { this.service=service; }
    @GetMapping public List<HistoryView> list(@PathVariable Long masteryId) { return service.history(masteryId).stream().map(HistoryView::from).toList(); }
}
