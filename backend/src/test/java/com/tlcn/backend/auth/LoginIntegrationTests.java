package com.tlcn.backend.auth;

import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired LoginService login;
    @Autowired LoginTokenService tokens;
    @MockitoBean Clock clock;
    @MockitoBean OtpDelivery delivery;
    private static final Instant START = Instant.parse("2026-11-14T00:00:00Z");
    private User user;

    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(START);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        var student = new Student();
        student.setEmail(java.util.UUID.randomUUID() + "@example.invalid");
        student.setFullName("Nguyễn An");
        student.setPassword(encoder.encode("password123"));
        student.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(student);
    }

    private record Csrf(MockHttpSession session, String header, String token) {}
    private Csrf csrf() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return new Csrf((MockHttpSession) result.getRequest().getSession(false), csrf.getHeaderName(), csrf.getToken());
    }
    private ResultActions attempt(String email, String password, boolean remember) throws Exception {
        var csrf = csrf();
        return mvc.perform(post("/api/auth/login").session(csrf.session()).header(csrf.header(), csrf.token())
                .contentType("application/json").content("""
                {"email":"%s","password":"%s","rememberMe":%s}
                """.formatted(email, password, remember)));
    }
    private Cookie authCookie(MvcResult result) {
        String header = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(h -> h.startsWith(LoginTokenService.COOKIE + "=") && !h.contains("Max-Age=0"))
                .findFirst().orElseThrow();
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        return new Cookie(LoginTokenService.COOKIE, header.split(";", 2)[0].split("=", 2)[1]);
    }

    @Test void emailLoginNormalizesEmailAndAuthenticatesWithoutReturningSecrets() throws Exception {
        var result = attempt(" " + user.getEmail().toUpperCase() + " ", "password123", false)
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.token").doesNotExist()).andReturn();
        Cookie cookie = authCookie(result);
        String header = result.getResponse().getHeaders("Set-Cookie").stream().filter(h -> !h.contains("Max-Age=0")).findFirst().orElseThrow();
        assertFalse(header.contains("Max-Age="));
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));
        mvc.perform(get("/api/users").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test void wrongPasswordAndUnknownEmailHaveTheSameError() throws Exception {
        attempt(user.getEmail(), "incorrect", false).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        attempt("unknown@example.invalid", "incorrect", false).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test void pendingAndLockedAccountsCannotLogin() throws Exception {
        user.setStatus(UserStatus.PENDING_OTP); users.saveAndFlush(user);
        attempt(user.getEmail(), "password123", false).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        user.setStatus(UserStatus.LOCKED); users.saveAndFlush(user);
        attempt(user.getEmail(), "password123", false).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test void rememberedJwtExpiresAtThirtyDaysAndOrdinaryJwtAtEightHours() throws Exception {
        Cookie ordinary = authCookie(attempt(user.getEmail(), "password123", false).andExpect(status().isOk()).andReturn());
        var remembered = attempt(user.getEmail(), "password123", true).andExpect(status().isOk()).andReturn();
        Cookie cookie = authCookie(remembered);
        assertTrue(remembered.getResponse().getHeaders("Set-Cookie").stream().anyMatch(h -> h.contains("Max-Age=2592000")));
        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(8)));
        mvc.perform(get("/api/auth/me").cookie(ordinary)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk());
        when(clock.instant()).thenReturn(START.plus(Duration.ofDays(30)));
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test void logoutRevokesJwtEvenIfTheOldCookieIsReplayed() throws Exception {
        Cookie cookie = authCookie(attempt(user.getEmail(), "password123", true).andExpect(status().isOk()).andReturn());
        var csrf = csrf();
        mvc.perform(post("/api/auth/logout").cookie(cookie).session(csrf.session()).header(csrf.header(), csrf.token()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test void lockedAccountAndTamperedCookieLoseAccessImmediately() throws Exception {
        Cookie cookie = authCookie(attempt(user.getEmail(), "password123", false).andExpect(status().isOk()).andReturn());
        Cookie tampered = new Cookie(LoginTokenService.COOKIE, cookie.getValue() + "tampered");
        mvc.perform(get("/api/auth/me").cookie(tampered)).andExpect(status().isUnauthorized());
        user.setStatus(UserStatus.LOCKED); users.saveAndFlush(user);
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test void csrfAndValidationProtectLoginAndLogout() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
        attempt("invalid", "", false).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void googleLoginRequiresAnExistingVerifiedSubjectAndNeverLinksByEmail() {
        assertEquals("GOOGLE_NOT_REGISTERED", assertThrows(AuthException.class,
                () -> login.loginGoogle("unknown", true)).getCode());
        user.setGoogleSubject("google-" + user.getEmail()); users.saveAndFlush(user);
        assertEquals(user.getId(), login.loginGoogle(user.getGoogleSubject(), true).getId());
        assertThrows(AuthException.class, () -> login.loginGoogle(user.getGoogleSubject(), false));
        assertThrows(AuthException.class, () -> login.login(new com.tlcn.backend.auth.dto.LoginRequest(user.getEmail(), "password123", false)));
        user.setStatus(UserStatus.LOCKED); users.saveAndFlush(user);
        assertEquals("ACCOUNT_LOCKED", assertThrows(AuthException.class,
                () -> login.loginGoogle(user.getGoogleSubject(), true)).getCode());
    }
}
