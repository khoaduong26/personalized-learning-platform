package com.tlcn.backend.mastery.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "mastery_history")
public class MasteryHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "mastery_id") private KnowledgeMastery mastery;
    private Double oldScore;
    @Column(nullable = false) private Double newScore;
    @Column(nullable = false) private Instant recordedAt = Instant.now();
    @Column(nullable = false, length = 120) private String triggerSource;
    protected MasteryHistory() {}
    public Long getId() { return id; }
    public KnowledgeMastery getMastery() { return mastery; }
    public void setMastery(KnowledgeMastery value) { mastery = value; }
    public Double getOldScore() { return oldScore; }
    public void setOldScore(Double value) { oldScore = value; }
    public Double getNewScore() { return newScore; }
    public void setNewScore(Double value) { newScore = value; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String value) { triggerSource = value; }
}
