package com.tlcn.backend.academic.entity;

import jakarta.persistence.*;
import com.tlcn.backend.user.entity.User;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "name" }))
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @OneToMany(mappedBy = "subject")
    private List<Topic> topics = new ArrayList<>();

    protected Subject() {
    }

    public Subject(String name) {
        this.name = name;
    }

    public Subject(String name, User owner) {
        this.name = name;
        this.owner = owner;
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

    public User getOwner() { return owner; }

    public void setOwner(User owner) { this.owner = owner; }

    public List<Topic> getTopics() {
        return topics;
    }
}
