package com.tlcn.backend.mastery.entity;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.user.entity.Student;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "knowledge_gaps")
public class KnowledgeGap {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lesson_id") private Lesson lesson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private GapPriority priority;
    @Column(columnDefinition = "TEXT") private String rootCause;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 25) private GapStatus status = GapStatus.IDENTIFIED;
    @Column(nullable = false) private Instant identifiedAt = Instant.now();
    protected KnowledgeGap() {}
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson value) { lesson = value; }
    public GapPriority getPriority() { return priority; }
    public void setPriority(GapPriority value) { priority = value; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String value) { rootCause = value; }
    public GapStatus getStatus() { return status; }
    public void setStatus(GapStatus value) { status = value; }
    public Instant getIdentifiedAt() { return identifiedAt; }
}
