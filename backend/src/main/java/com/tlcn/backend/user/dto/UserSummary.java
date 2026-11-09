package com.tlcn.backend.user.dto;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.entity.Role;
public record UserSummary(Long id, String fullName, String email, Role role) {
    public static UserSummary from(User user) { return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole()); }
}
