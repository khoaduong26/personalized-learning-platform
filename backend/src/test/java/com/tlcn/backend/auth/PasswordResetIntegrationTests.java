package com.tlcn.backend.auth;

import com.jayway.jsonpath.JsonPath;
import com.tlcn.backend.auth.dto.ResetPasswordRequest;
import com.tlcn.backend.auth.dto.VerifyOtpRequest;
import com.tlcn.backend.auth.entity.UserGoogleIdentity;
import com.tlcn.backend.auth.exception.AuthException;
import com.tlcn.backend.auth.repository.UserGoogleIdentityRepository;
import com.tlcn.backend.auth.service.OtpDelivery;
import com.tlcn.backend.auth.service.serviceImpl.PasswordResetService;
import com.tlcn.backend.auth.util.LoginTokenCookie;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetIntegrationTests {
    private static final Instant START = Instant.parse("2026-11-26T00:00:00Z");
    private static final String OLD_PASSWORD = "original-password";
    private static final String NEW_PASSWORD = "replacement-password";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserGoogleIdentityRepository googleIdentities;
    @Autowired PasswordEncoder encoder;
    @Autowired PasswordResetService resets;
    @MockitoBean Clock clock;
    @MockitoBean OtpDelivery delivery;
    private Student user;

    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(START);
        var student = new Student();
        student.setFullName("Học sinh");
        student.setEmail(UUID.randomUUID() + "@example.invalid");
        student.setPassword(encoder.encode(OLD_PASSWORD));
        user = users.saveAndFlush(student);
    }

    @Test void resetsThroughPublicEndpointsAndRevokesEveryPreviousSession() throws Exception {
        Cookie firstSession = loginCookie(login(OLD_PASSWORD).andExpect(status().isOk()).andReturn());
        Cookie secondSession = loginCookie(login(OLD_PASSWORD).andExpect(status().isOk()).andReturn());
        mvc.perform(get("/api/auth/me").cookie(firstSession)).andExpect(status().isOk());

        String unknown = postJson("forgot-password", "{\"email\":\"unknown@example.invalid\"}")
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String known = postJson("forgot-password", "{\"email\":\"%s\"}".formatted(user.getEmail()))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        assertEquals(unknown, known);
        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(delivery, timeout(3000)).sendPasswordResetCode(eq(user.getEmail()), otp.capture());
        assertTrue(otp.getValue().matches("[0-9]{6}"));

        String verifyBody = "{\"email\":\"%s\",\"otp\":\"%s\"}".formatted(user.getEmail(), otp.getValue());
        String grant = postJson("verify-reset-otp", verifyBody).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        postJson("verify-reset-otp", verifyBody).andExpect(status().isBadRequest());
        String token = JsonPath.read(grant, "$.resetToken");

        postJson("reset-password", resetBody("A".repeat(43), NEW_PASSWORD)).andExpect(status().isBadRequest());
        postJson("reset-password", resetBody(token, OLD_PASSWORD)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_CONFIRMATION_MISMATCH"));
        postJson("reset-password", resetBody(token, NEW_PASSWORD)).andExpect(status().isNoContent());
        postJson("reset-password", resetBody(token, NEW_PASSWORD)).andExpect(status().isBadRequest());
        verify(delivery, timeout(3000)).sendPasswordResetNotice(user.getEmail());
        mvc.perform(get("/api/auth/me").cookie(firstSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(secondSession)).andExpect(status().isUnauthorized());
        login(OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test void limitsGuessesAndInvalidatesResentAndExpiredCodes() {
        String email = user.getEmail();
        String first = resets.issueOtp(email).orElseThrow().otp();
        assertTrue(resets.issueOtp(email).isEmpty());
        String wrong = first.equals("123456") ? "123457" : "123456";
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(AuthException.class, () -> resets.verifyOtp(new VerifyOtpRequest(email, wrong)));
        }
        assertThrows(AuthException.class, () -> resets.verifyOtp(new VerifyOtpRequest(email, first)));

        when(clock.instant()).thenReturn(START.plusSeconds(60));
        String second = resets.issueOtp(email).orElseThrow().otp();
        assertNotEquals(first, second);
        assertThrows(AuthException.class, () -> resets.verifyOtp(new VerifyOtpRequest(email, first)));
        when(clock.instant()).thenReturn(START.plusSeconds(360));
        assertThrows(AuthException.class, () -> resets.verifyOtp(new VerifyOtpRequest(email, second)));

        googleIdentities.saveAndFlush(new UserGoogleIdentity(user, UUID.randomUUID().toString()));
        assertTrue(resets.issueOtp(email).isEmpty());
    }

    @Test void exactlyOneConcurrentResetCanConsumeTheGrant() throws Exception {
        String email = user.getEmail();
        String otp = resets.issueOtp(email).orElseThrow().otp();
        String token = resets.verifyOtp(new VerifyOtpRequest(email, otp)).resetToken();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attemptReset(start, token, "first-password"));
            var second = executor.submit(() -> attemptReset(start, token, "second-password"));
            start.countDown();
            assertNotEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }
    }

    private boolean attemptReset(CountDownLatch start, String token, String password) throws InterruptedException {
        start.await();
        try {
            resets.resetPassword(new ResetPasswordRequest(token, password, password));
            return true;
        } catch (AuthException ex) {
            assertEquals("RESET_TOKEN_INVALID", ex.getCode());
            return false;
        }
    }

    private ResultActions login(String password) throws Exception {
        return postJson("login", "{\"email\":\"%s\",\"password\":\"%s\",\"rememberMe\":false}"
                .formatted(user.getEmail(), password));
    }

    private Cookie loginCookie(MvcResult result) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(cookie -> cookie.getName().equals(LoginTokenCookie.NAME) && cookie.getMaxAge() != 0)
                .findFirst().orElseThrow();
    }

    private ResultActions postJson(String path, String body) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return mvc.perform(post("/api/auth/" + path)
                .session((MockHttpSession) result.getRequest().getSession(false))
                .header(csrf.getHeaderName(), csrf.getToken()).contentType("application/json").content(body));
    }

    private String resetBody(String token, String confirmation) {
        return "{\"resetToken\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}"
                .formatted(token, NEW_PASSWORD, confirmation);
    }
}
