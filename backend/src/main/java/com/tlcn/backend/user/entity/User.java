package com.tlcn.backend.user.entity;

import jakarta.persistence.*;

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
    // Preserve the original users columns and their insert defaults for existing databases.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Embedded
    private LegacyAuth legacyAuth = new LegacyAuth();
    protected User() {}
    public static User withRole(Role role) {
        return switch (role) {
            case STUDENT -> new Student();
            case TEACHER -> new Teacher();
            case ADMIN -> {
                User user = new User();
                user.setRole(Role.ADMIN);
                yield user;
            }
        };
    }
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
}
