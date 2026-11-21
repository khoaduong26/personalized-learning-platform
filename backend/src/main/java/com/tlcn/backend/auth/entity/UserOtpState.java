package com.tlcn.backend.auth.entity;

import com.tlcn.backend.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_otp_state")
public class UserOtpState {
    @Id
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String otpCode;
    private Instant otpExpiredAt;
    private Instant otpSentAt;
    private Instant otpWindowStartedAt;
    private int otpSendCount;
    private int otpAttempts;

    protected UserOtpState() {}
    public UserOtpState(User user) { this.user = user; }

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

    public void clearChallenge() {
        otpCode = null;
        otpExpiredAt = null;
        otpAttempts = 0;
    }

    public void reset() {
        clearChallenge();
        otpSentAt = null;
        otpWindowStartedAt = null;
        otpSendCount = 0;
    }
}
