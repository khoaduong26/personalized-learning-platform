package com.tlcn.backend.user.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120) private String fullName;
    @Column(nullable = false, unique = true, length = 190) private String email;
    @Column(nullable = false) private String password;
    private String phone;
    private String avatarUrl;
    private String address;
    private String city;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserStatus status = UserStatus.ACTIVE;
    // Only hashes are persisted; neither passwords nor OTPs may be serialized.
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String otpCode;
    private Instant otpExpiredAt;
    private Instant otpSentAt;
    private Instant otpWindowStartedAt;
    private int otpSendCount;
    private int otpAttempts;
    @Column(unique = true, length = 255)
    private String googleSubject;
    protected User() {}
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public Instant getOtpExpiredAt() { return otpExpiredAt; }
    public void setOtpExpiredAt(Instant otpExpiredAt) { this.otpExpiredAt = otpExpiredAt; }
    public Instant getOtpSentAt() { return otpSentAt; }
    public void setOtpSentAt(Instant value) { otpSentAt = value; }
    public Instant getOtpWindowStartedAt() { return otpWindowStartedAt; }
    public void setOtpWindowStartedAt(Instant value) { otpWindowStartedAt = value; }
    public int getOtpSendCount() { return otpSendCount; }
    public void setOtpSendCount(int value) { otpSendCount = value; }
    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int value) { otpAttempts = value; }
    public String getGoogleSubject() { return googleSubject; }
    public void setGoogleSubject(String value) { googleSubject = value; }
}
