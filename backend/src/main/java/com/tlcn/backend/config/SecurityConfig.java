package com.tlcn.backend.config;

import com.tlcn.backend.auth.service.GoogleRegistrationSuccessHandler;
import com.tlcn.backend.auth.service.LoginTokenService;
import com.tlcn.backend.auth.config.LoginTokenFilter;
import com.tlcn.backend.auth.controller.AuthController;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean Clock authClock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, GoogleRegistrationSuccessHandler googleSuccessHandler,
            LoginTokenService tokens,
            ObjectProvider<ClientRegistrationRepository> clients,
            @Value("${app.frontend-url}") String frontendUrl) throws Exception {
        http.cors(cors -> {})
            // These public endpoints do not use cookie-based authentication. Keep CSRF for other routes.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/resend-otp"))
            .authorizeHttpRequests(rules -> rules
                .requestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/resend-otp",
                        "/api/auth/options", "/api/auth/google", "/api/auth/google-login",
                        "/api/auth/login", "/api/auth/logout", "/api/auth/csrf", "/oauth2/authorization/google",
                        "/login/oauth2/code/google", "/error").permitAll()
                .requestMatchers("/api/auth/registration-session", "/api/auth/me").authenticated()
                // Existing feature stubs have no ownership checks yet. Do not expose student data.
                .anyRequest().denyAll())
            .requestCache(cache -> cache.disable())
            .logout(logout -> logout.disable())
            .addFilterBefore(new LoginTokenFilter(tokens), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, ex) -> {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Bạn cần đăng nhập.\"}");
            }));
        if (clients.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                .successHandler(googleSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    boolean login = request.getSession(false) != null
                            && "login".equals(request.getSession(false).getAttribute(AuthController.GOOGLE_FLOW));
                    SecurityContextHolder.clearContext();
                    if (request.getSession(false) != null) request.getSession(false).invalidate();
                    response.sendRedirect(frontendUrl + (login ? "/login" : "/register") + "?google=error");
                }));
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.frontend-url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontendUrl));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
