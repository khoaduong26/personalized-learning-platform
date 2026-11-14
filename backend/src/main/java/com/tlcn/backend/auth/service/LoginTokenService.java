package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.entity.LoginSession;
import com.tlcn.backend.auth.repository.LoginSessionRepository;
import com.tlcn.backend.user.dto.UserSummary;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.*;

@Service
public class LoginTokenService {
    public static final String COOKIE = "TLCN_AUTH";
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final LoginSessionRepository sessions;
    private final UserRepository users;
    private final Clock clock;
    private final boolean secure;

    public LoginTokenService(LoginSessionRepository sessions, UserRepository users, Clock clock,
            @Value("${app.auth.jwt-secret:}") String configuredSecret,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.sessions = sessions; this.users = users; this.clock = clock; this.secure = secure;
        byte[] secret;
        if (configuredSecret.isBlank()) {
            secret = new byte[32]; new java.security.SecureRandom().nextBytes(secret);
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("JWT_SECRET is unset: using an ephemeral development key. Logins expire on restart.");
        } else {
            try { secret = Base64.getDecoder().decode(configuredSecret); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("JWT_SECRET must be Base64 encoded", ex); }
            if (secret.length < 32) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes");
        }
        var key = new SecretKeySpec(secret, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // Use the injected clock and enforce exact expiry, including in tests.
        decoder.setJwtValidator(jwt -> {
            boolean valid = "https://tlcn.local".equals(jwt.getIssuer() == null ? null : jwt.getIssuer().toString())
                    && jwt.getExpiresAt() != null && clock.instant().isBefore(jwt.getExpiresAt());
            return valid ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                    : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        });
    }

    @Transactional
    public void issue(User user, boolean rememberMe, HttpServletResponse response) {
        Instant now = clock.instant();
        sessions.deleteExpired(now);
        Duration lifetime = rememberMe ? Duration.ofDays(30) : Duration.ofHours(8);
        String id = UUID.randomUUID().toString();
        sessions.save(new LoginSession(id, user.getId(), now.plus(lifetime)));
        var claims = JwtClaimsSet.builder().issuer("https://tlcn.local").subject(user.getId().toString())
                .id(id).issuedAt(now).expiresAt(now.plus(lifetime)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        var cookie = ResponseCookie.from(COOKIE, token).httpOnly(true).secure(secure).sameSite("Lax").path("/");
        if (rememberMe) cookie.maxAge(lifetime);
        response.addHeader("Set-Cookie", cookie.build().toString());
    }

    @Transactional(readOnly = true)
    public Optional<UserSummary> authenticate(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return sessions.findById(jwt.getId())
                    .filter(s -> clock.instant().isBefore(s.getExpiresAt()))
                    .filter(s -> s.getUserId().toString().equals(jwt.getSubject()))
                    .flatMap(s -> users.findById(s.getUserId()))
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE).map(UserSummary::from);
        } catch (JwtException | IllegalArgumentException ex) { return Optional.empty(); }
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        token(request).ifPresent(token -> {
            try { sessions.deleteById(decoder.decode(token).getId()); }
            catch (JwtException | IllegalArgumentException ignored) { /* Invalid tokens are already unusable. */ }
        });
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        response.addHeader("Set-Cookie", ResponseCookie.from(COOKIE, "").httpOnly(true).secure(secure)
                .sameSite("Lax").path("/").maxAge(0).build().toString());
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    public static Optional<String> token(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies()).filter(c -> COOKIE.equals(c.getName())).map(Cookie::getValue).findFirst();
    }
}
