package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.dto.LoginRequest;
import com.tlcn.backend.user.entity.*;

public interface LoginService {
    User login(LoginRequest request);
    User loginGoogle(String subject, Boolean verified);
}
