
package com.tlcn.backend.config;

import com.tlcn.backend.auth.handler.GoogleRegistrationSuccessHandler;
import com.tlcn.backend.auth.service.LoginTokenService;
import com.tlcn.backend.auth.config.LoginTokenFilter;
import com.tlcn.backend.auth.controller.AuthController;

import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpMethod;
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
        http.cors(cors -> {})
            // These public endpoints do not use cookie-based authentication. Keep CSRF for other routes.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/resend-otp"))
            .authorizeHttpRequests(rules -> rules
                .requestMatchers("/api/auth/register", "/api/auth/verify-otp", "/api/auth/resend-otp",
                        "/api/auth/forgot-password", "/api/auth/verify-reset-otp", "/api/auth/reset-password",
                        "/api/auth/options", "/api/auth/google", "/api/auth/google-login",
                        "/api/auth/login", "/api/auth/logout", "/api/auth/csrf", "/api/csrf", "/oauth2/authorization/google",
                        "/login/oauth2/code/google", "/error").permitAll()
                .requestMatchers("/api/auth/registration-session", "/api/auth/me", "/api/auth/session").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/me").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers(HttpMethod.PUT, "/api/users/me", "/api/users/me/password").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/users/me/avatar").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/admin/accounts", "/api/admin/accounts/**").hasRole("ADMIN")
                // User directory is reserved for the admin user-management workflow.
                .requestMatchers("/api/users", "/api/users/*").hasRole("ADMIN")
                // Classroom service methods enforce teacher ownership and admin access.
                .requestMatchers("/api/classrooms/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students/candidates").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students/*/mastery/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/students/*/learning-paths/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/students/*").hasAnyRole("ADMIN", "TEACHER")
                // Curriculum is readable by signed-in users; only admins may change shared content.
                .requestMatchers(HttpMethod.GET, "/api/subjects/**", "/api/topics/**", "/api/lessons/**")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/subjects/**", "/api/topics/**", "/api/lessons/**")
                    .hasAnyRole("ADMIN", "TEACHER")
                // Service checks assignment, teacher ownership, attempt ownership and review role.
                .requestMatchers("/api/assessments/**", "/api/attempts/**")
                    .hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                .requestMatchers("/api/mastery/classrooms/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/mastery/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                // The service checks student, teacher-classroom, and admin ownership.
                // Learning-path service enforces student and teacher-classroom ownership.
                // Exercise service methods enforce step ownership and teacher-only review.
                .requestMatchers("/api/exercises/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
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
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", cors);

        return source;
    }
}
