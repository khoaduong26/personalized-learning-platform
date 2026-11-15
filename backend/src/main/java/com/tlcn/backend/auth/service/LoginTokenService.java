package com.tlcn.backend.auth.service;

import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.*;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface LoginTokenService {
    void issue(User user, boolean rememberMe, HttpServletResponse response);
    Optional<UserSummary> authenticate(String token);
    void logout(HttpServletRequest request, HttpServletResponse response);
}
