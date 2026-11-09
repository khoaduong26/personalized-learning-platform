package com.tlcn.backend.learningpath.entity;
import com.tlcn.backend.user.entity.Student;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name = "learning_paths")
public class LearningPath {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private LearningPathStatus status = LearningPathStatus.ACTIVE;
    @Column(nullable = false) private Integer version = 1;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("stepOrder ASC") private List<LearningStep> steps = new ArrayList<>();
    protected LearningPath() {}
    public Long getId() { return id; }
    public LearningPathStatus getStatus() { return status; }
    public void setStatus(LearningPathStatus value) { status = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public List<LearningStep> getSteps() { return steps; }
}
