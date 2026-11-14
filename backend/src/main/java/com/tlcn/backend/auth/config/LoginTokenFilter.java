package com.tlcn.backend.auth.config;

import com.tlcn.backend.auth.service.LoginTokenService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// Constructed inside the security chain: never register a second servlet filter.
public class LoginTokenFilter extends OncePerRequestFilter {
    private final LoginTokenService tokens;
    public LoginTokenFilter(LoginTokenService tokens) { this.tokens = tokens; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // App identity always comes from a live JWT, not Spring's preliminary OIDC session.
        SecurityContextHolder.clearContext();
        LoginTokenService.token(request).flatMap(tokens::authenticate).ifPresent(user -> {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
            SecurityContextHolder.setContext(context);
        });
        chain.doFilter(request, response);
    }
}
