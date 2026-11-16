package com.tlcn.backend.user.service;

import com.tlcn.backend.user.dto.*;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    ProfileResponse getProfile(Long userId);
    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
    ProfileResponse updateAvatar(Long userId, MultipartFile file);
    void changePassword(Long userId, ChangePasswordRequest request);
}
