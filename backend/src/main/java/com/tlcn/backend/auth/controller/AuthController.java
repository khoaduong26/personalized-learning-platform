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
    public static final String GOOGLE_FLOW = "googleFlow";
    public static final String GOOGLE_REMEMBER = "googleRemember";
    private final AuthService service;
    private final LoginService login;
    private final LoginTokenService tokens;
    private final ObjectProvider<ClientRegistrationRepository> clients;
    public AuthController(AuthService service, ObjectProvider<ClientRegistrationRepository> clients,
            LoginService login, LoginTokenService tokens) {
        this.service = service;
        this.clients = clients;
        this.login = login;
        this.tokens = tokens;
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

    @GetMapping("/csrf")
    public Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken csrf) {
        return Map.of("token", csrf.getToken(), "headerName", csrf.getHeaderName());
    }

    @PostMapping("/login")
    public UserSummary login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        var user = login.login(body);
        // Revoke an existing login when switching accounts and rotate the CSRF session.
        tokens.logout(request, response);
        tokens.issue(user, body.rememberMe(), response);
        return UserSummary.from(user);
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal UserSummary user) { return user; }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) { tokens.logout(request, response); }

    @GetMapping("/google-login")
    public void googleLogin(@RequestParam(defaultValue = "false") boolean rememberMe,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        requireGoogle();
        request.getSession().removeAttribute(GOOGLE_ROLE);
        request.getSession().setAttribute(GOOGLE_FLOW, "login");
        request.getSession().setAttribute(GOOGLE_REMEMBER, rememberMe);
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/google")
    public void google(@RequestParam Role role, HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.requirePublicRole(role);
        requireGoogle();
        request.getSession().setAttribute(GOOGLE_FLOW, "register");
        request.getSession().removeAttribute(GOOGLE_REMEMBER);
        request.getSession().setAttribute(GOOGLE_ROLE, role);
        response.sendRedirect("/oauth2/authorization/google");
    }

    private void requireGoogle() {
        if (clients.getIfAvailable() == null) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "GOOGLE_UNAVAILABLE", "Google chưa được cấu hình.");
        }
    }
}
