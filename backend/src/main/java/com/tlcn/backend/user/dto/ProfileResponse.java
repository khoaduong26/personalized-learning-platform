package com.tlcn.backend.user.dto;

import com.tlcn.backend.user.entity.*;
import java.time.LocalDate;

public record ProfileResponse(Long id, String fullName, String email, String phone, String avatarUrl,
        String address, String city, String sex, LocalDate dateOfBirth, Role role, boolean passwordChangeAllowed) {
    public static ProfileResponse from(User user) {
        Student student = user instanceof Student value ? value : null;
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getAvatarUrl(), user.getAddress(), user.getCity(),
                student == null ? null : student.getSex(), student == null ? null : student.getDateOfBirth(),
                user.getRole(), user.getGoogleSubject() == null);
    }
}
