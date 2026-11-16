package com.tlcn.backend.user;

import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.auth.util.LoginTokenCookie;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.exception.ProfileException;
import com.tlcn.backend.user.repository.UserRepository;
import com.tlcn.backend.user.service.AvatarStorage;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired LoginTokenService tokens;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean Clock clock;
    @MockitoBean OtpDelivery delivery;
    @MockitoBean AvatarStorage avatars;
    private User user;
    private Cookie cookie;
    private static final String PROFILE = """
            {"fullName":"  Nguyễn An  ","phone":"0901234567","address":"  12 Nguyễn Huệ  ",
             "city":" Hồ Chí Minh ","sex":"FEMALE","dateOfBirth":"2018-05-20"}
            """;

    @BeforeEach void setup() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-16T00:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        user = create(Role.STUDENT);
        cookie = issue(user);
    }

    private User create(Role role) {
        User result;
        if (role == Role.STUDENT) result = new Student();
        else if (role == Role.TEACHER) result = new Teacher();
        else {
            try {
                var constructor = User.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
                result.setRole(Role.ADMIN);
            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        }
        result.setFullName("Tên ban đầu");
        result.setEmail(java.util.UUID.randomUUID() + "@example.invalid");
        result.setPassword(encoder.encode("password123"));
        return users.saveAndFlush(result);
    }

    private Cookie issue(User actor) {
        var response = new MockHttpServletResponse();
        tokens.issue(actor, false, response);
        String header = response.getHeader("Set-Cookie");
        return new Cookie(LoginTokenCookie.NAME, header.split(";", 2)[0].split("=", 2)[1]);
    }

    private record Csrf(MockHttpSession session, String header, String token) {}
    private Csrf csrf() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = (CsrfToken) result.getRequest().getAttribute(CsrfToken.class.getName());
        return new Csrf((MockHttpSession) result.getRequest().getSession(false), token.getHeaderName(), token.getToken());
    }

    private ResultActions putJson(String path, String body, Cookie auth) throws Exception {
        var csrf = csrf();
        return mvc.perform(put(path).cookie(auth).session(csrf.session()).header(csrf.header(), csrf.token())
                .contentType("application/json").content(body));
    }

    private ResultActions upload(MockMultipartFile file) throws Exception {
        var csrf = csrf();
        return mvc.perform(multipart("/api/users/me/avatar").file(file).cookie(cookie)
                .session(csrf.session()).header(csrf.header(), csrf.token()));
    }

    private MockMultipartFile image() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("file", "avatar.png", "image/png", output.toByteArray());
    }

    @Test void allRolesCanReadAndUpdateOnlyTheirOwnProfile() throws Exception {
        for (Role role : Role.values()) {
            User actor = create(role);
            Cookie auth = issue(actor);
            mvc.perform(get("/api/users/me").cookie(auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(actor.getId())).andExpect(jsonPath("$.role").value(role.name()))
                    .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.otpCode").doesNotExist())
                    .andExpect(jsonPath("$.googleSubject").doesNotExist());
            String body = role == Role.STUDENT ? PROFILE : PROFILE
                    .replace("\"FEMALE\"", "null").replace("\"2018-05-20\"", "null");
            putJson("/api/users/me", body, auth).andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Nguyễn An"));
            mvc.perform(get("/api/users/me").cookie(auth)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.address").value("12 Nguyễn Huệ"))
                    .andExpect(jsonPath("$.city").value("Hồ Chí Minh"));
            User persisted = users.findById(actor.getId()).orElseThrow();
            if (persisted instanceof Student student) {
                assertEquals(LocalDate.of(2018, 5, 20), student.getDateOfBirth());
                assertEquals("FEMALE", student.getSex());
                mvc.perform(get("/api/users/me").cookie(auth)).andExpect(status().isOk())
                        .andExpect(jsonPath("$.sex").value("FEMALE"))
                        .andExpect(jsonPath("$.dateOfBirth").value("2018-05-20"));
            } else {
                mvc.perform(get("/api/users/me").cookie(auth)).andExpect(status().isOk())
                        .andExpect(jsonPath("$.sex").isEmpty()).andExpect(jsonPath("$.dateOfBirth").isEmpty());
            }
            assertEquals(role, persisted.getRole());
        }
        assertEquals("Tên ban đầu", users.findById(user.getId()).orElseThrow().getFullName());
    }

    @Test void studentFieldsRemainOnlyInStudentsAndAreRejectedForOtherRoles() throws Exception {
        assertEquals(0, jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'PUBLIC' and table_name = 'USERS' and column_name in ('SEX', 'DATE_OF_BIRTH')
                """, Integer.class));
        assertEquals(2, jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'PUBLIC' and table_name = 'STUDENTS' and column_name in ('SEX', 'DATE_OF_BIRTH')
                """, Integer.class));
        for (Role role : new Role[]{Role.ADMIN, Role.TEACHER}) {
            User actor = create(role);
            Cookie auth = issue(actor);
            for (String field : new String[]{"\"sex\":\"FEMALE\"", "\"dateOfBirth\":\"2018-05-20\""}) {
                putJson("/api/users/me", "{\"fullName\":\"Tên mới\"," + field + "}", auth)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("STUDENT_FIELDS_NOT_ALLOWED"));
                assertEquals("Tên ban đầu", users.findById(actor.getId()).orElseThrow().getFullName());
            }
        }
    }

    @Test void identityAndSecurityFieldsCannotBeChangedThroughProfile() throws Exception {
        String originalHash = user.getPassword();
        putJson("/api/users/me", """
                {"fullName":"Nguyễn Bình","id":999999,"email":"attacker@example.invalid","role":"ADMIN",
                 "status":"LOCKED","password":"injected","googleSubject":"injected","avatarUrl":"https://evil.invalid/a.png"}
                """, cookie).andExpect(status().isOk());
        User persisted = users.findById(user.getId()).orElseThrow();
        assertEquals(user.getEmail(), persisted.getEmail());
        assertEquals(Role.STUDENT, persisted.getRole());
        assertEquals(UserStatus.ACTIVE, persisted.getStatus());
        assertEquals(originalHash, persisted.getPassword());
        assertNull(persisted.getGoogleSubject());
        assertNull(persisted.getAvatarUrl());
        mvc.perform(get("/api/users/" + user.getId()).cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").cookie(cookie)).andExpect(status().isForbidden());
    }

    @Test void validationDoesNotPartiallySaveChanges() throws Exception {
        putJson("/api/users/me", """
                {"fullName":"Tên thay đổi","phone":"abc","sex":"INVALID","dateOfBirth":"2999-01-01"}
                """, cookie).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists()).andExpect(jsonPath("$.fieldErrors.sex").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
        putJson("/api/users/me", "{\"fullName\":\" \"}", cookie).andExpect(status().isBadRequest());
        putJson("/api/users/me", "{\"fullName\":\"Tên hợp lệ\",\"dateOfBirth\":\"bad-date\"}", cookie)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertEquals("Tên ban đầu", users.findById(user.getId()).orElseThrow().getFullName());
    }

    @Test void missingOrInactiveAuthenticationAndMissingCsrfCannotModifyProfile() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        var csrf = csrf();
        mvc.perform(put("/api/users/me").session(csrf.session()).header(csrf.header(), csrf.token())
                .contentType("application/json").content(PROFILE)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me").cookie(cookie).contentType("application/json").content(PROFILE))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/me/password").cookie(cookie).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/users/me/avatar").file(image()).cookie(cookie)).andExpect(status().isForbidden());
        for (UserStatus state : new UserStatus[]{UserStatus.LOCKED, UserStatus.PENDING_OTP}) {
            user.setStatus(state); users.saveAndFlush(user);
            mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isUnauthorized());
            putJson("/api/users/me", PROFILE, cookie).andExpect(status().isUnauthorized());
        }
    }

    @Test void passwordChangeRequiresCurrentPasswordAndConfirmation() throws Exception {
        String hash = user.getPassword();
        putJson("/api/users/me/password", """
                {"currentPassword":"incorrect","newPassword":"newPassword123","confirmPassword":"newPassword123"}
                """, cookie).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_INVALID"));
        putJson("/api/users/me/password", """
                {"currentPassword":"password123","newPassword":"newPassword123","confirmPassword":"different123"}
                """, cookie).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.confirmPassword").exists());
        putJson("/api/users/me/password", """
                {"currentPassword":"password123","newPassword":"password123","confirmPassword":"password123"}
                """, cookie).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
        putJson("/api/users/me/password", """
                {"currentPassword":"password123","newPassword":"short","confirmPassword":"short"}
                """, cookie).andExpect(status().isBadRequest());
        assertEquals(hash, users.findById(user.getId()).orElseThrow().getPassword());
        mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isOk());
    }

    @Test void passwordChangeHashesPasswordAndRevokesAllSessions() throws Exception {
        Cookie anotherSession = issue(user);
        User other = create(Role.TEACHER);
        Cookie otherCookie = issue(other);
        putJson("/api/users/me/password", """
                {"currentPassword":"password123","newPassword":"newPassword123","confirmPassword":"newPassword123"}
                """, cookie).andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        String hash = users.findById(user.getId()).orElseThrow().getPassword();
        assertTrue(encoder.matches("newPassword123", hash));
        assertFalse(encoder.matches("password123", hash));
        mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").cookie(anotherSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").cookie(otherCookie)).andExpect(status().isOk());
        var csrf = csrf();
        mvc.perform(post("/api/auth/login").session(csrf.session()).header(csrf.header(), csrf.token())
                .contentType("application/json").content("""
                {"email":"%s","password":"newPassword123","rememberMe":false}
                """.formatted(user.getEmail()))).andExpect(status().isOk());
    }

    @Test void googleAccountCanEditProfileButCannotChangeLocalPassword() throws Exception {
        user.setGoogleSubject("google-" + user.getEmail()); users.saveAndFlush(user);
        mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordChangeAllowed").value(false));
        putJson("/api/users/me", PROFILE, cookie).andExpect(status().isOk());
        putJson("/api/users/me/password", """
                {"currentPassword":"password123","newPassword":"newPassword123","confirmPassword":"newPassword123"}
                """, cookie).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("GOOGLE_PASSWORD_MANAGED"));
    }

    @Test void avatarUploadPersistsOnlyAfterProviderSuccess() throws Exception {
        String previous = "https://res.cloudinary.com/demo/image/upload/old.png";
        String next = "https://res.cloudinary.com/demo/image/upload/new.png";
        user.setAvatarUrl(previous); users.saveAndFlush(user);
        when(avatars.upload(eq(user.getId()), any(byte[].class), eq("image/png")))
                .thenThrow(new ProfileException(HttpStatus.BAD_GATEWAY, "AVATAR_UPLOAD_FAILED", "Lỗi tải ảnh"));
        upload(image()).andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("AVATAR_UPLOAD_FAILED"));
        assertEquals(previous, users.findById(user.getId()).orElseThrow().getAvatarUrl());
        when(avatars.upload(eq(user.getId()), any(byte[].class), eq("image/png"))).thenReturn(next);
        upload(image()).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(next));
        mvc.perform(get("/api/users/me").cookie(cookie)).andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").value(next));
    }

    @Test void invalidEmptyOversizedOrFakeImagesAreRejectedBeforeStorage() throws Exception {
        upload(new MockMultipartFile("file", "a.png", "image/png", new byte[0])).andExpect(status().isBadRequest());
        upload(new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes())).andExpect(status().isBadRequest());
        upload(new MockMultipartFile("file", "a.svg", "image/svg+xml", "<svg/>".getBytes())).andExpect(status().isBadRequest());
        upload(new MockMultipartFile("file", "a.jpg", "image/jpeg", image().getBytes())).andExpect(status().isBadRequest());
        upload(new MockMultipartFile("file", "a.png", "image/png", new byte[2 * 1024 * 1024 + 1]))
                .andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.code").value("AVATAR_TOO_LARGE"));
        var csrf = csrf();
        mvc.perform(multipart("/api/users/me/avatar").cookie(cookie).session(csrf.session())
                .header(csrf.header(), csrf.token())).andExpect(status().isBadRequest());
        verifyNoInteractions(avatars);
    }
}
