package com.tlcn.backend.auth;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTests {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    @MockitoBean OtpDelivery delivery;
    @MockitoBean Clock clock;
    private final AtomicReference<String> sentOtp = new AtomicReference<>();
    private static final Instant START = Instant.parse("2026-11-13T00:00:00Z");
    private String email;

    @BeforeEach
    void setup() {
        email = java.util.UUID.randomUUID() + "@example.invalid";
        when(clock.instant()).thenReturn(START);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        doAnswer(invocation -> { sentOtp.set(invocation.getArgument(1)); return null; })
                .when(delivery).send(anyString(), anyString());
    }

    private RegisterRequest request(Role role) {
        return new RegisterRequest("  Nguyễn An  ", " " + email.toUpperCase() + " ", "password123", "0901234567", role);
    }
    private User user() { return users.findByEmail(email).orElseThrow(); }
    private void assertCode(String code, Runnable operation) {
        assertEquals(code, assertThrows(AuthException.class, operation::run).getCode());
    }

    @Test
    void teacherRegistrationPersistsSubtypeAndHashesThenActivatesExactlyOnce() {
        var response = auth.register(request(Role.TEACHER));
        User pending = user();
        assertEquals(UserStatus.PENDING_OTP, response.status());
        assertEquals("Nguyễn An", pending.getFullName());
        assertTrue(teachers.existsById(pending.getId()));
        assertTrue(encoder.matches("password123", pending.getPassword()));
        assertNotEquals(sentOtp.get(), pending.getOtpCode());
        assertTrue(encoder.matches(sentOtp.get(), pending.getOtpCode()));
        assertEquals(START.plusSeconds(300), response.otpExpiresAt());
        auth.verify(new VerifyOtpRequest(email, sentOtp.get()));
        assertEquals(UserStatus.ACTIVE, user().getStatus());
        assertNull(user().getOtpCode());
        assertNull(user().getOtpExpiredAt());
        assertCode("REGISTRATION_NOT_PENDING", () -> auth.verify(new VerifyOtpRequest(email, sentOtp.get())));
    }

    @Test
    void studentCannotEscalateToAdminAndDuplicateEmailCannotReplacePassword() {
        assertCode("ROLE_INVALID", () -> auth.register(request(Role.ADMIN)));
        assertTrue(users.findByEmail(email).isEmpty());
        auth.register(request(Role.STUDENT));
        User pending = user();
        assertTrue(students.existsById(pending.getId()));
        String original = pending.getPassword();
        assertCode("EMAIL_EXISTS", () -> auth.register(request(Role.TEACHER)));
        assertEquals(original, user().getPassword());
        assertEquals(Role.STUDENT, user().getRole());
        verify(delivery, times(1)).send(eq(email), anyString());
    }

    @Test
    void failedOtpAttemptsCommitAndBlockEvenTheCorrectCodeAfterFiveFailures() {
        auth.register(request(Role.STUDENT));
        String correct = sentOtp.get();
        String wrong = correct.equals("000000") ? "111111" : "000000";
        for (int i = 1; i <= 5; i++) {
            assertCode("OTP_INVALID", () -> auth.verify(new VerifyOtpRequest(email, wrong)));
            assertEquals(i, user().getOtpAttempts());
        }
        assertCode("OTP_ATTEMPTS_EXCEEDED", () -> auth.verify(new VerifyOtpRequest(email, correct)));
        assertEquals(UserStatus.PENDING_OTP, user().getStatus());
    }

    @Test
    void otpExpiresAtTheBoundaryAndResendInvalidatesOldChallenge() {
        auth.register(request(Role.STUDENT));
        String oldHash = user().getOtpCode();
        String oldOtp = sentOtp.get();
        assertCode("OTP_COOLDOWN", () -> auth.resend(new EmailRequest(email)));
        when(clock.instant()).thenReturn(START.plusSeconds(300));
        assertCode("OTP_EXPIRED", () -> auth.verify(new VerifyOtpRequest(email, sentOtp.get())));
        var response = auth.resend(new EmailRequest(email));
        assertNotEquals(oldHash, user().getOtpCode());
        assertEquals(0, user().getOtpAttempts());
        assertEquals(START.plusSeconds(600), response.otpExpiresAt());
        assertCode("OTP_INVALID", () -> auth.verify(new VerifyOtpRequest(email, oldOtp)));
        auth.verify(new VerifyOtpRequest(email, sentOtp.get()));
        assertEquals(UserStatus.ACTIVE, user().getStatus());
    }

    @Test
    void sendingIsLimitedToFivePerHourThenRecovers() {
        auth.register(request(Role.TEACHER));
        for (int i = 1; i < 5; i++) {
            when(clock.instant()).thenReturn(START.plusSeconds(i * 60L));
            auth.resend(new EmailRequest(email));
        }
        when(clock.instant()).thenReturn(START.plusSeconds(300));
        assertCode("OTP_SEND_LIMIT", () -> auth.resend(new EmailRequest(email)));
        when(clock.instant()).thenReturn(START.plusSeconds(3600));
        auth.resend(new EmailRequest(email));
        assertEquals(1, user().getOtpSendCount());
    }

    @Test
    void mailFailureRollsBackRegistrationAndKeepsPreviousOtpOnResend() {
        doThrow(new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Unavailable"))
                .when(delivery).send(anyString(), anyString());
        assertCode("MAIL_UNAVAILABLE", () -> auth.register(request(Role.STUDENT)));
        assertTrue(users.findByEmail(email).isEmpty());
        doNothing().when(delivery).send(anyString(), anyString());
        auth.register(request(Role.STUDENT));
        String originalHash = user().getOtpCode();
        when(clock.instant()).thenReturn(START.plusSeconds(60));
        doThrow(new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Unavailable"))
                .when(delivery).send(anyString(), anyString());
        assertCode("MAIL_UNAVAILABLE", () -> auth.resend(new EmailRequest(email)));
        assertEquals(originalHash, user().getOtpCode());
        assertEquals(1, user().getOtpSendCount());
    }

    @Test
    void lockedOrActiveAccountsCannotBeReactivatedViaOtp() {
        auth.register(request(Role.STUDENT));
        User locked = user();
        locked.setStatus(UserStatus.LOCKED);
        users.saveAndFlush(locked);
        assertCode("REGISTRATION_NOT_PENDING", () -> auth.resend(new EmailRequest(email)));
        assertCode("REGISTRATION_NOT_PENDING", () -> auth.verify(new VerifyOtpRequest(email, sentOtp.get())));
        assertEquals(UserStatus.LOCKED, user().getStatus());
    }

    @Test
    void googleRequiresVerifiedEmailAndNeverLinksExistingAccounts() {
        assertCode("GOOGLE_UNVERIFIED", () -> auth.registerGoogle("subject-" + email, email, false, "An", Role.STUDENT));
        assertTrue(users.findByEmail(email).isEmpty());
        User created = auth.registerGoogle("subject-" + email, email, true, "An", Role.TEACHER);
        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertTrue(teachers.existsById(created.getId()));
        assertCode("EMAIL_EXISTS", () -> auth.registerGoogle("another-" + email, email, true, "An", Role.STUDENT));
        assertEquals(Role.TEACHER, user().getRole());
        assertCode("EMAIL_EXISTS", () -> auth.registerGoogle("subject-" + email, "other-" + email, true, "An", Role.TEACHER));
    }

    @Test
    void apiValidatesInputsDoesNotExposeSecretsAndProtectsExistingData() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                {"fullName":"A","email":"invalid","password":"short","phone":"abc","role":"STUDENT"}
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists());
        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                {"fullName":"Nguyễn An","email":"%s","password":"password123","phone":"0901234567","role":"STUDENT"}
                """.formatted(email))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_OTP"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.otpCode").doesNotExist());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/registration-session")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/options")).andExpect(status().isOk()).andExpect(jsonPath("$.googleEnabled").value(false));
        mvc.perform(get("/api/auth/google").param("role", "ADMIN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/google").param("role", "STUDENT")).andExpect(status().isServiceUnavailable());
    }
}
