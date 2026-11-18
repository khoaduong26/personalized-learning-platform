
package com.tlcn.backend.config;

import com.tlcn.backend.auth.handler.GoogleRegistrationSuccessHandler;
import com.tlcn.backend.auth.service.LoginTokenService;
import com.tlcn.backend.auth.config.LoginTokenFilter;
import com.tlcn.backend.auth.controller.AuthController;

import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GoogleRegistrationSuccessHandler googleSuccessHandler,
            LoginTokenService tokens,
            ObjectProvider<ClientRegistrationRepository> clients,
            @Value("${app.frontend-url}") String frontendUrl) throws Exception {

        http
                .cors(cors -> {
                })

                // CSRF protection
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(
                                new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-otp",
                                "/api/auth/resend-otp"))

                // Authorization rules
                .authorizeHttpRequests(rules -> rules

                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify-otp",
                                "/api/auth/resend-otp",
                                "/api/auth/options",
                                "/api/auth/google",
                                "/api/auth/google-login",
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/csrf",
                                "/api/csrf",
                                "/oauth2/authorization/google",
                                "/login/oauth2/code/google",
                                "/error")
                        .permitAll()

                        // Authenticated endpoints
                        .requestMatchers(
                                "/api/auth/registration-session",
                                "/api/auth/session",
                                "/api/auth/me")
                        .authenticated()

                        // Academic management
                        .requestMatchers(
                                "/api/subjects/**",
                                "/api/topics/**",
                                "/api/lessons/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // Classroom and student management
                        .requestMatchers(
                                "/api/classrooms/**",
                                "/api/students/**")
                        .hasAnyRole("ADMIN", "TEACHER")

                        // Deny requests that are not explicitly permitted
                        .anyRequest().denyAll())

                .requestCache(cache -> cache.disable())

                // Custom logout via LoginTokenService
                .logout(logout -> logout.disable())

                // Authentication using login token
                .addFilterBefore(
                        new LoginTokenFilter(tokens),
                        UsernamePasswordAuthenticationFilter.class)

                // JSON response for unauthenticated requests
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, ex) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json;charset=UTF-8");
                                    response.getWriter().write(
                                            "{\"code\":\"UNAUTHORIZED\","
                                                    + "\"message\":\"Bạn cần đăng nhập.\"}");
                                }));

        // Configure Google OAuth2 if available
        if (clients.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .successHandler(googleSuccessHandler)
                    .failureHandler((request, response, exception) -> {

                        boolean signingIn = request.getSession(false) != null
                                && "login".equals(
                                        request.getSession(false)
                                                .getAttribute(AuthController.GOOGLE_FLOW));

                        SecurityContextHolder.clearContext();

                        if (request.getSession(false) != null) {
                            request.getSession(false).invalidate();
                        }

                        response.sendRedirect(
                                frontendUrl
                                        + (signingIn ? "/login" : "/register")
                                        + "?google=error");
                    }));
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-url}") String frontendUrl) {
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(List.of(frontendUrl));

        cors.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"));

        cors.setAllowedHeaders(List.of(
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-CSRF-TOKEN"));

        cors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cors);

        return source;
    }
}
