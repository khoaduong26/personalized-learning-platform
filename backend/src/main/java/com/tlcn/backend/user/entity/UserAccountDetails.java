package com.tlcn.backend.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_account_details")
public class UserAccountDetails {
    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus statusBeforeLock;

    private String formerStudentSex;
    private LocalDate formerStudentDateOfBirth;

    protected UserAccountDetails() {}

    public UserAccountDetails(User user, Instant createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public UserStatus getStatusBeforeLock() { return statusBeforeLock; }
    public void setStatusBeforeLock(UserStatus statusBeforeLock) { this.statusBeforeLock = statusBeforeLock; }
    public String getFormerStudentSex() { return formerStudentSex; }
    public LocalDate getFormerStudentDateOfBirth() { return formerStudentDateOfBirth; }

    public void rememberStudentDetails(String sex, LocalDate dateOfBirth) {
        this.formerStudentSex = sex;
        this.formerStudentDateOfBirth = dateOfBirth;
    }
}
