package com.tlcn.backend.auth.controller;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.dto.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String GOOGLE_ROLE = "registrationRole";
    private final AuthService service;
    private final ObjectProvider<ClientRegistrationRepository> clients;
    public AuthController(AuthService service, ObjectProvider<ClientRegistrationRepository> clients) {
        this.service = service;
        this.clients = clients;
    }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }

    @PostMapping("/verify-otp")
    public RegistrationResponse verify(@Valid @RequestBody VerifyOtpRequest request) { return service.verify(request); }

    @PostMapping("/resend-otp")
    public RegistrationResponse resend(@Valid @RequestBody EmailRequest request) { return service.resend(request); }

    @GetMapping("/options")
    public Map<String, Boolean> options() { return Map.of("googleEnabled", clients.getIfAvailable() != null); }

    @GetMapping("/registration-session")
    public UserSummary registrationSession(@AuthenticationPrincipal UserSummary user) { return user; }

    @GetMapping("/google")
    public void google(@RequestParam Role role, HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.requirePublicRole(role);
        if (clients.getIfAvailable() == null) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_UNAVAILABLE", "Đăng ký Google chưa được cấu hình.");
        }
        request.getSession().setAttribute(GOOGLE_ROLE, role);
        response.sendRedirect("/oauth2/authorization/google");
    }
}
