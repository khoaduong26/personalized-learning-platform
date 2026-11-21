package com.tlcn.backend.auth.entity;

import com.tlcn.backend.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_google_identity")
public class UserGoogleIdentity {
    @Id
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "google_subject", nullable = false, unique = true, length = 255)
    private String subject;

    protected UserGoogleIdentity() {}

    public UserGoogleIdentity(User user, String subject) {
        this.user = user;
        this.subject = subject;
    }
}
