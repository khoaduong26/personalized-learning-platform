package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.user.entity.*;

public interface AuthService {
    RegistrationResponse register(RegisterRequest request);

    RegistrationResponse verify(VerifyOtpRequest request);

    RegistrationResponse resend(EmailRequest request);

    User registerGoogle(String subject, String email, Boolean verified, String name, Role role);

    void requirePublicRole(Role role);
}
