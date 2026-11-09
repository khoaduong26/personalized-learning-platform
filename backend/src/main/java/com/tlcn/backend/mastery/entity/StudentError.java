package com.tlcn.backend.mastery.entity;
import com.tlcn.backend.academic.entity.Lesson;
import com.tlcn.backend.assessment.entity.StudentAnswer;
import com.tlcn.backend.user.entity.Student;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "student_errors")
public class StudentError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id") private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lesson_id") private Lesson lesson;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "answer_id") private StudentAnswer answer;
    @Column(nullable = false, length = 180) private String errorType;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    protected StudentError() {}
    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student value) { student = value; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson value) { lesson = value; }
    public StudentAnswer getAnswer() { return answer; }
    public void setAnswer(StudentAnswer value) { answer = value; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String value) { errorType = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Instant getCreatedAt() { return createdAt; }
}
