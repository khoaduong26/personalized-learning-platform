package com.tlcn.backend.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tlcn.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_otp_state")
public class UserOtpState {
    public enum Purpose { REGISTRATION, PASSWORD_RESET }

    @Id
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // Rows created before purpose was introduced belong to registration.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Purpose purpose;
    @JsonIgnore
    private String otpCode;
    private Instant otpExpiredAt;
    private Instant otpSentAt;
    private Instant otpWindowStartedAt;
    private int otpSendCount;
    private int otpAttempts;
    private Integer resetAttemptsInWindow;
    @JsonIgnore
    @Column(length = 64)
    private String credentialTag;
    @JsonIgnore
    @Column(length = 64, unique = true)
    private String resetTokenHash;
    private Instant resetTokenExpiresAt;

    protected UserOtpState() {}
    public UserOtpState(User user, Purpose purpose) {
        this.user = user;
        this.purpose = purpose;
    }

    public boolean isFor(Purpose requested) {
        return purpose == requested || (purpose == null && requested == Purpose.REGISTRATION);
    }

    public void usePurpose(Purpose requested) {
        if (!isFor(requested)) {
            reset();
        }
        purpose = requested;
    }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String value) { otpCode = value; }
    public Instant getOtpExpiredAt() { return otpExpiredAt; }
    public void setOtpExpiredAt(Instant value) { otpExpiredAt = value; }
    public Instant getOtpSentAt() { return otpSentAt; }
    public void setOtpSentAt(Instant value) { otpSentAt = value; }
    public Instant getOtpWindowStartedAt() { return otpWindowStartedAt; }
    public void setOtpWindowStartedAt(Instant value) { otpWindowStartedAt = value; }
    public int getOtpSendCount() { return otpSendCount; }
    public void setOtpSendCount(int value) { otpSendCount = value; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int value) { otpAttempts = value; }
    public int getResetAttemptsInWindow() { return resetAttemptsInWindow == null ? 0 : resetAttemptsInWindow; }
    public void setResetAttemptsInWindow(int value) { resetAttemptsInWindow = value; }
    public String getCredentialTag() { return credentialTag; }
    public void setCredentialTag(String value) { credentialTag = value; }
    public String getResetTokenHash() { return resetTokenHash; }
    public void setResetTokenHash(String value) { resetTokenHash = value; }
    public Instant getResetTokenExpiresAt() { return resetTokenExpiresAt; }
    public void setResetTokenExpiresAt(Instant value) { resetTokenExpiresAt = value; }

    public void clearChallenge() {
        otpCode = null;
        otpExpiredAt = null;
        otpAttempts = 0;
        credentialTag = null;
        resetTokenHash = null;
        resetTokenExpiresAt = null;
    }

    public void reset() {
        clearChallenge();
        otpSentAt = null;
        otpWindowStartedAt = null;
        otpSendCount = 0;
        resetAttemptsInWindow = null;
    }
}
