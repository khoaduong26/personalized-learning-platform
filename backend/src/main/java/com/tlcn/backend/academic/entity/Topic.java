package com.tlcn.backend.academic.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "topics", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "name"}))
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 160) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id") private Subject subject;
    @OneToMany(mappedBy = "topic") private List<Lesson> lessons = new ArrayList<>();
    protected Topic() {}
    public Topic(String name, Subject subject) { this.name = name; this.subject = subject; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public List<Lesson> getLessons() { return lessons; }
}
