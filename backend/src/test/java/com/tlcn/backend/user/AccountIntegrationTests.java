package com.tlcn.backend.user;

import com.tlcn.backend.auth.util.LoginTokenCookie;
import com.tlcn.backend.classroom.entity.Classroom;
import com.tlcn.backend.classroom.repository.ClassroomRepository;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.Teacher;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.entity.UserAccountDetails;
import com.tlcn.backend.user.entity.UserStatus;
import com.tlcn.backend.user.repository.StudentRepository;
import com.tlcn.backend.user.repository.TeacherRepository;
import com.tlcn.backend.user.repository.UserAccountDetailsRepository;
import com.tlcn.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrationTests {
    private static final String ACCOUNTS = "/api/admin/accounts";
    private static final String PASSWORD = "initial-password";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired UserAccountDetailsRepository accountDetails;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @Autowired ClassroomRepository classrooms;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    private User admin;
    private Cookie adminSession;

    @BeforeEach
    void signInAsAdmin() throws Exception {
        admin = persist(Role.ADMIN, UserStatus.ACTIVE);
        adminSession = login(admin.getEmail());
    }

    @Test
    void createsAndFindsAccountsWithAdminOnlyAccess() throws Exception {
        String idPart = UUID.randomUUID().toString();
        String email = idPart + "@example.invalid";
        String body = """
                {"fullName":"  Test Account  ","email":" %s ","phone":"+84901234567",
                 "role":"TEACHER","password":"%s"}
                """.formatted(email.toUpperCase(java.util.Locale.ROOT), PASSWORD);

        mvc.perform(get(ACCOUNTS)).andExpect(status().isUnauthorized());
        var student = persist(Role.STUDENT, UserStatus.ACTIVE);
        Cookie studentSession = login(student.getEmail());
        mvc.perform(get(ACCOUNTS).cookie(studentSession)).andExpect(status().isForbidden());
        submit(post(ACCOUNTS), studentSession, body).andExpect(status().isForbidden());
        mvc.perform(post(ACCOUNTS).cookie(adminSession).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        var created = submit(post(ACCOUNTS), adminSession, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Test Account"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        String location = created.getResponse().getHeader("Location");
        assertNotNull(location);
        mvc.perform(get(location).cookie(adminSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("+84901234567"));
        mvc.perform(get(ACCOUNTS).cookie(adminSession).param("keyword", idPart)
                        .param("role", "TEACHER").param("status", "ACTIVE").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].email").value(email))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(ACCOUNTS).cookie(adminSession).param("page", "-1")).andExpect(status().isBadRequest());
        submit(post(ACCOUNTS), adminSession, body)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        assertNotNull(login(email));
    }

    @Test
    void roleAndLockChangesRevokeSessionsAndProvideTheCorrectProfile() throws Exception {
        Student target = (Student) persist(Role.STUDENT, UserStatus.ACTIVE);
        LocalDate birthday = LocalDate.of(2018, 4, 15);
        Instant createdAt = Instant.parse("2026-11-21T12:00:00Z");
        accountDetails.saveAndFlush(new UserAccountDetails(target, createdAt));
        target.setDateOfBirth(birthday);
        target.setSex("FEMALE");
        students.saveAndFlush(target);
        Cookie oldSession = login(target.getEmail());
        submit(put(ACCOUNTS + "/" + target.getId()), adminSession, updateBody(target, Role.TEACHER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TEACHER"));
        assertTrue(students.findById(target.getId()).isEmpty());
        assertTrue(teachers.findById(target.getId()).isPresent());
        mvc.perform(get("/api/auth/me").cookie(oldSession)).andExpect(status().isUnauthorized());

        Cookie teacherSession = login(target.getEmail());
        mvc.perform(get("/api/auth/me").cookie(teacherSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("TEACHER"));
        setLocked(target, true).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LOCKED"));
        loginRequest(target.getEmail()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        setLocked(target, false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(get("/api/auth/me").cookie(teacherSession)).andExpect(status().isUnauthorized());
        assertNotNull(login(target.getEmail()));
        submit(put(ACCOUNTS + "/" + target.getId()), adminSession, updateBody(target, Role.STUDENT))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"));
        var restored = students.findById(target.getId()).orElseThrow();
        assertEquals(createdAt, accountDetails.findById(target.getId()).orElseThrow().getCreatedAt());
        assertEquals(birthday, restored.getDateOfBirth());
        assertEquals("FEMALE", restored.getSex());
    }

    @Test
    void linkedDataAndPendingRegistrationSurviveAccountChanges() throws Exception {
        Teacher teacher = (Teacher) persist(Role.TEACHER, UserStatus.ACTIVE);
        classrooms.saveAndFlush(new Classroom("UC07 class " + teacher.getId(), "2026-2027", teacher));
        submit(put(ACCOUNTS + "/" + teacher.getId()), adminSession, updateBody(teacher, Role.STUDENT))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ROLE_IN_USE"));
        assertEquals(Role.TEACHER, users.findById(teacher.getId()).orElseThrow().getRole());
        assertTrue(students.findById(teacher.getId()).isEmpty());

        User pending = persist(Role.STUDENT, UserStatus.PENDING_OTP);
        setLocked(pending, true).andExpect(status().isOk());
        setLocked(pending, false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_OTP"));
        loginRequest(pending.getEmail()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        setLocked(admin, true).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_LOCK"));
        submit(put(ACCOUNTS + "/" + admin.getId()), adminSession, updateBody(admin, Role.TEACHER))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE"));
    }

    @Test
    void legacyAccountsWithoutDetailsCanBeLockedWithoutAddingColumnsToUsers() throws Exception {
        Integer extraColumns = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'PUBLIC' and table_name = 'USERS'
                and column_name in ('CREATED_AT', 'STATUS_BEFORE_LOCK', 'FORMER_STUDENT_SEX',
                                    'FORMER_STUDENT_DATE_OF_BIRTH')
                """, Integer.class);
        assertEquals(0, extraColumns);

        User pending = persist(Role.STUDENT, UserStatus.PENDING_OTP);
        mvc.perform(get(ACCOUNTS + "/" + pending.getId()).cookie(adminSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").isEmpty());
        setLocked(pending, true).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LOCKED"));
        assertEquals("PENDING_OTP", jdbc.queryForObject(
                "select status_before_lock from user_account_details where user_id = ?", String.class, pending.getId()));
        setLocked(pending, false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_OTP"));
        mvc.perform(get(ACCOUNTS + "/" + pending.getId()).cookie(adminSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").isEmpty());
    }

    private User persist(Role role, UserStatus status) {
        User user = User.withRole(role);
        user.setFullName("Test Account");
        user.setEmail(UUID.randomUUID() + "@example.invalid");
        user.setPassword(passwords.encode(PASSWORD));
        user.setStatus(status);
        return users.saveAndFlush(user);
    }

    private String updateBody(User user, Role role) {
        return """
                {"fullName":"Updated Account","email":"%s","role":"%s"}
                """.formatted(user.getEmail(), role);
    }

    private ResultActions setLocked(User user, boolean locked) throws Exception {
        return submit(patch(ACCOUNTS + "/" + user.getId() + "/status"), adminSession, "{\"locked\":" + locked + "}");
    }

    private ResultActions loginRequest(String email) throws Exception {
        return submit(post("/api/auth/login"), null, """
                {"email":"%s","password":"%s","rememberMe":false}
                """.formatted(email, PASSWORD));
    }

    private Cookie login(String email) throws Exception {
        var result = loginRequest(email).andExpect(status().isOk()).andReturn();
        String value = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith(LoginTokenCookie.NAME + "=") && !header.contains("Max-Age=0"))
                .findFirst().orElseThrow().split(";", 2)[0].split("=", 2)[1];
        return new Cookie(LoginTokenCookie.NAME, value);
    }

    private ResultActions submit(MockHttpServletRequestBuilder request, Cookie cookie, String body) throws Exception {
        var csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var csrf = (CsrfToken) csrfResult.getRequest().getAttribute(CsrfToken.class.getName());
        if (cookie != null) request.cookie(cookie);
        return mvc.perform(request.session((MockHttpSession) csrfResult.getRequest().getSession(false))
                .header(csrf.getHeaderName(), csrf.getToken()).contentType("application/json").content(body));
    }
}
