package com.tlcn.backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
class LegacyAuth {
    @Column(insertable = false, updatable = false)
    private String otpCode;
    @Column(insertable = false, updatable = false)
    private Instant otpExpiredAt;
    @Column(insertable = false, updatable = false)
    private Instant otpSentAt;
    @Column(insertable = false, updatable = false)
    private Instant otpWindowStartedAt;
    @Column(nullable = false, updatable = false)
    private int otpSendCount;
    @Column(nullable = false, updatable = false)
    private int otpAttempts;
    @Column(insertable = false, updatable = false, unique = true, length = 255)
    private String googleSubject;
}
