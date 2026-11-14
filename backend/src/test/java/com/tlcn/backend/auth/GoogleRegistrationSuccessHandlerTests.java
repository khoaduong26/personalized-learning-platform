package com.tlcn.backend.auth;

import com.tlcn.backend.auth.controller.AuthController;
import com.tlcn.backend.auth.service.*;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleRegistrationSuccessHandlerTests {
    private final AuthService auth = mock(AuthService.class);
    private final GoogleRegistrationSuccessHandler handler = new GoogleRegistrationSuccessHandler(auth, "http://localhost:5173");
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final OidcUser oidcUser = mock(OidcUser.class);

    @BeforeEach void setup() {
        when(oidcUser.getSubject()).thenReturn("subject");
        when(oidcUser.getEmail()).thenReturn("test@example.invalid");
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getFullName()).thenReturn("An");
        request.getSession().setAttribute(AuthController.GOOGLE_ROLE, Role.STUDENT);
    }
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    private void complete() throws Exception {
        handler.onAuthenticationSuccess(request, response,
                UsernamePasswordAuthenticationToken.authenticated(oidcUser, null, List.of()));
    }

    @Test void successfulRegistrationCreatesSessionWithOnlyTheStoredRoleAndSafePrincipal() throws Exception {
        Student student = new Student();
        student.setFullName("An");
        student.setEmail("test@example.invalid");
        student.setPassword("sensitive hash");
        when(auth.registerGoogle("subject", "test@example.invalid", true, "An", Role.STUDENT)).thenReturn(student);
        complete();
        assertEquals("http://localhost:5173/register?google=success", response.getRedirectedUrl());
        assertNull(request.getSession().getAttribute(AuthController.GOOGLE_ROLE));
        assertNotNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(UserSummary.class, authentication.getPrincipal());
        assertNull(authentication.getCredentials());
        assertEquals(List.of("ROLE_STUDENT"), authentication.getAuthorities().stream().map(Object::toString).toList());
    }

    @Test void existingAccountDoesNotKeepThePreliminaryGoogleSession() throws Exception {
        when(auth.registerGoogle("subject", "test@example.invalid", true, "An", Role.STUDENT))
                .thenThrow(new AuthException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Exists"));
        complete();
        assertEquals("http://localhost:5173/register?google=exists", response.getRedirectedUrl());
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void unexpectedPersistenceFailureAlsoClearsAuthentication() throws Exception {
        when(auth.registerGoogle("subject", "test@example.invalid", true, "An", Role.STUDENT))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Database unavailable"));
        complete();
        assertEquals("http://localhost:5173/register?google=error", response.getRedirectedUrl());
        assertNull(request.getSession(false));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test void callbackWithoutSelectedRoleCannotCreateAnAccount() throws Exception {
        request.getSession().removeAttribute(AuthController.GOOGLE_ROLE);
        complete();
        verifyNoInteractions(auth);
        assertEquals("http://localhost:5173/register?google=error", response.getRedirectedUrl());
        assertNull(request.getSession(false));
    }
}
