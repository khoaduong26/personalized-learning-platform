package com.tlcn.backend.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_sessions", indexes = @Index(name = "idx_login_session_expiry", columnList = "expiresAt"))
public class LoginSession {
    @Id private String id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false) private Instant expiresAt;
    protected LoginSession() {}
    public LoginSession(String id, Long userId, Instant expiresAt) {
        this.id = id; this.userId = userId; this.expiresAt = expiresAt;
    }
    public Long getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
