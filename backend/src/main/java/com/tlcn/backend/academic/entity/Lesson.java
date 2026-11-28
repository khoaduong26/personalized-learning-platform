package com.tlcn.backend.academic.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons", uniqueConstraints = @UniqueConstraint(columnNames = { "topic_id", "name" }))
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "learning_content", columnDefinition = "TEXT")
    private String learningContent;
    @ElementCollection
    @CollectionTable(name = "lesson_images", joinColumns = @JoinColumn(name = "lesson_id"))
    @OrderColumn(name = "display_order")
    private List<LessonImage> images = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    protected Lesson() {
    }

    public Lesson(String name, String description, Topic topic) {
        this(name, description, null, topic);
    }

    public Lesson(String name, String description, String learningContent, Topic topic) {
        this.name = name;
        this.description = description;
        this.learningContent = learningContent;
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLearningContent() {
        return learningContent;
    }

    public void setLearningContent(String learningContent) {
        this.learningContent = learningContent;
    }

    public List<LessonImage> getImages() { return images; }

    public void setImages(List<LessonImage> images) {
        if (this.images == null) this.images = new ArrayList<>();
        else this.images.clear();
        if (images != null) this.images.addAll(images);
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}
