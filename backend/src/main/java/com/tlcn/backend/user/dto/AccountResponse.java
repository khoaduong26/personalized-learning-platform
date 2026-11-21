package com.tlcn.backend.user.dto;

import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record AccountResponse(Long id, String fullName, String email, String phone,
        Role role, UserStatus status, Instant createdAt) {
    public static AccountResponse from(User user, Instant createdAt) {
        return new AccountResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), createdAt);
    }

    public record Listing(List<AccountResponse> items, int page, int size, long totalElements, int totalPages) {
        public static Listing from(Page<AccountResponse> result) {
            return new Listing(result.getContent(), result.getNumber(), result.getSize(),
                    result.getTotalElements(), result.getTotalPages());
        }
    }
}
