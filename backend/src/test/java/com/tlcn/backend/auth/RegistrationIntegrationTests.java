package com.tlcn.backend.auth;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.auth.entity.UserOtpState;
import com.tlcn.backend.auth.entity.UserOtpState.Purpose;
import com.tlcn.backend.auth.repository.UserOtpStateRepository;
import com.tlcn.backend.auth.exception.AuthException;
import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.auth.service.serviceImpl.PasswordResetService;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Autowired PasswordResetService resets;
    @Autowired UserRepository users;
    @Autowired UserOtpStateRepository otpStates;
    @Autowired JdbcTemplate jdbc;
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
    private UserOtpState otpState() { return otpStates.findById(user().getId()).orElseThrow(); }
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
        assertNotEquals(sentOtp.get(), otpState().getOtpCode());
        assertTrue(encoder.matches(sentOtp.get(), otpState().getOtpCode()));
        assertNull(jdbc.queryForObject("select otp_code from users where id = ?", String.class, pending.getId()));
        assertEquals(2, jdbc.queryForObject("""
                select count(*) from information_schema.columns where table_schema = 'PUBLIC'
                and table_name = 'USERS' and column_name in ('OTP_ATTEMPTS', 'OTP_SEND_COUNT')
                and is_nullable = 'NO'
                """, Integer.class));
        assertEquals(START.plusSeconds(300), response.otpExpiresAt());
        auth.verify(new VerifyOtpRequest(email, sentOtp.get()));
        assertEquals(UserStatus.ACTIVE, user().getStatus());
        assertNull(otpState().getOtpCode());
        assertNull(otpState().getOtpExpiredAt());
        assertCode("REGISTRATION_NOT_PENDING", () -> auth.verify(new VerifyOtpRequest(email, sentOtp.get())));
    }

    @Test
    void sharedOtpStateSupportsLegacyRegistrationThenPasswordResetForTheSameAccount() {
        auth.register(request(Role.STUDENT));
        Long userId = user().getId();
        String registrationOtp = sentOtp.get();
        jdbc.update("update user_otp_state set purpose = null where user_id = ?", userId);

        assertTrue(resets.issueOtp(email).isEmpty());
        auth.verify(new VerifyOtpRequest(email, registrationOtp));
        assertTrue(otpState().isFor(Purpose.REGISTRATION));
        assertCode("OTP_INVALID", () -> resets.verifyOtp(new VerifyOtpRequest(email, registrationOtp)));

        // A registration send just occurred, but its cooldown does not apply to password reset.
        String otp = resets.issueOtp(email).orElseThrow().otp();
        assertTrue(otpStates.findById(userId).orElseThrow().isFor(Purpose.PASSWORD_RESET));
        assertEquals(1, otpState().getOtpSendCount());
        String token = resets.verifyOtp(new VerifyOtpRequest(email, otp)).resetToken();
        resets.resetPassword(new ResetPasswordRequest(token, "new-password", "new-password"));
        assertTrue(encoder.matches("new-password", user().getPassword()));
        assertNull(otpState().getResetTokenHash());
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'PUBLIC' and table_name = 'PASSWORD_RESET_STATES'
                """, Integer.class));
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
            assertEquals(i, otpState().getOtpAttempts());
        }
        assertCode("OTP_ATTEMPTS_EXCEEDED", () -> auth.verify(new VerifyOtpRequest(email, correct)));
        assertEquals(UserStatus.PENDING_OTP, user().getStatus());
    }

    @Test
    void otpExpiresAtTheBoundaryAndResendInvalidatesOldChallenge() {
        auth.register(request(Role.STUDENT));
        String oldHash = otpState().getOtpCode();
        String oldOtp = sentOtp.get();
        assertCode("OTP_COOLDOWN", () -> auth.resend(new EmailRequest(email)));
        when(clock.instant()).thenReturn(START.plusSeconds(300));
        assertCode("OTP_EXPIRED", () -> auth.verify(new VerifyOtpRequest(email, sentOtp.get())));
        var response = auth.resend(new EmailRequest(email));
        assertNotEquals(oldHash, otpState().getOtpCode());
        assertEquals(0, otpState().getOtpAttempts());
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
        assertEquals(1, otpState().getOtpSendCount());
    }

    @Test
    void mailFailureRollsBackRegistrationAndKeepsPreviousOtpOnResend() {
        doThrow(new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Unavailable"))
                .when(delivery).send(anyString(), anyString());
        assertCode("MAIL_UNAVAILABLE", () -> auth.register(request(Role.STUDENT)));
        assertTrue(users.findByEmail(email).isEmpty());
        doNothing().when(delivery).send(anyString(), anyString());
        auth.register(request(Role.STUDENT));
        String originalHash = otpState().getOtpCode();
        when(clock.instant()).thenReturn(START.plusSeconds(60));
        doThrow(new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Unavailable"))
                .when(delivery).send(anyString(), anyString());
        assertCode("MAIL_UNAVAILABLE", () -> auth.resend(new EmailRequest(email)));
        assertEquals(originalHash, otpState().getOtpCode());
        assertEquals(1, otpState().getOtpSendCount());
    }

    @Test
    void pendingAccountWithoutAnOtpStateCanRequestANewCode() {
        auth.register(request(Role.STUDENT));
        Long id = user().getId();
        String legacyCode = sentOtp.get();
        String legacyHash = otpState().getOtpCode();
        otpStates.deleteById(id);
        jdbc.update("""
                update users set otp_code = ?, otp_expired_at = ?, otp_sent_at = ?, otp_window_started_at = ?,
                    otp_send_count = 3, otp_attempts = 2 where id = ?
                """, legacyHash, START.plusSeconds(300).atOffset(ZoneOffset.UTC), START.atOffset(ZoneOffset.UTC),
                START.atOffset(ZoneOffset.UTC), id);

        assertCode("OTP_EXPIRED", () -> auth.verify(new VerifyOtpRequest(email, legacyCode)));
        var response = auth.resend(new EmailRequest(email));
        assertEquals(START.plusSeconds(300), response.otpExpiresAt());
        assertEquals(0, otpState().getOtpAttempts());
        assertEquals(1, otpState().getOtpSendCount());
        auth.verify(new VerifyOtpRequest(email, sentOtp.get()));
        assertEquals(UserStatus.ACTIVE, user().getStatus());
        assertNull(otpState().getOtpCode());
        assertEquals(legacyHash, jdbc.queryForObject("select otp_code from users where id = ?", String.class, id));
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
        assertNull(jdbc.queryForObject("select google_subject from users where id = ?", String.class, created.getId()));
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
