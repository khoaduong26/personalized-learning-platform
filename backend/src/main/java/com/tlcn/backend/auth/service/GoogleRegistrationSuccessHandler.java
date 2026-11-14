package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.controller.AuthController;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
public class GoogleRegistrationSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService auth;
    private final String frontendUrl;
    public GoogleRegistrationSuccessHandler(AuthService auth, @Value("${app.frontend-url}") String frontendUrl) {
        this.auth = auth;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String result = "success";
        try {
            HttpSession session = request.getSession(false);
            Object role = session == null ? null : session.getAttribute(AuthController.GOOGLE_ROLE);
            if (!(role instanceof Role selectedRole)) throw new IllegalArgumentException("Missing registration role");
            OidcUser principal = (OidcUser) authentication.getPrincipal();
            var user = auth.registerGoogle(principal.getSubject(), principal.getEmail(),
                    principal.getEmailVerified(), principal.getFullName(), selectedRole);
            session.removeAttribute(AuthController.GOOGLE_ROLE);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(UserSummary.from(user), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        } catch (AuthException ex) {
            result = ex.getCode().equals("EMAIL_EXISTS") ? "exists" : "error";
        } catch (RuntimeException ex) {
            // A persistence/provider failure must never leave Spring's preliminary OIDC login active.
            result = "error";
        }
        if (!result.equals("success")) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false) != null) request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendUrl + "/register?google=" + result);
    }
}
