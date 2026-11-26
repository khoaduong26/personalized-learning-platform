package com.tlcn.backend.auth.service.serviceImpl;

import com.tlcn.backend.auth.dto.ResetPasswordRequest;
import com.tlcn.backend.auth.dto.VerifyOtpRequest;
import com.tlcn.backend.auth.entity.UserOtpState;
import com.tlcn.backend.auth.entity.UserOtpState.Purpose;
import com.tlcn.backend.auth.exception.AuthException;
import com.tlcn.backend.auth.exception.InvalidOtpException;
import com.tlcn.backend.auth.repository.LoginSessionRepository;
import com.tlcn.backend.auth.repository.UserGoogleIdentityRepository;
import com.tlcn.backend.auth.repository.UserOtpStateRepository;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.entity.UserStatus;
import com.tlcn.backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int MAX_SENDS = 5;
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int MAX_ACCOUNT_ATTEMPTS = 15;

    private final UserRepository users;
    private final UserOtpStateRepository states;
    private final UserGoogleIdentityRepository googleIdentities;
    private final LoginSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec otpKey;

    public record ResetCode(String email, String otp) {}
    public record ResetGrant(String resetToken, Instant expiresAt) {
        @Override
        public String toString() {
            return "ResetGrant[redacted]";
        }
    }

    public PasswordResetService(UserRepository users, UserOtpStateRepository states,
            UserGoogleIdentityRepository googleIdentities, LoginSessionRepository sessions,
            PasswordEncoder encoder, Clock clock, @Value("${app.auth.password-reset-secret:}") String secret) {
        this.users = users;
        this.states = states;
        this.googleIdentities = googleIdentities;
        this.sessions = sessions;
        this.encoder = encoder;
        this.clock = clock;
        byte[] bytes = secret.isBlank() ? random.generateSeed(32) : Base64.getDecoder().decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("PASSWORD_RESET_SECRET must contain at least 32 random bytes");
        }
        otpKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Transactional
    public Optional<ResetCode> issueOtp(String email) {
        User user = users.findByEmailForUpdate(email).orElse(null);
        if (!eligible(user)) {
            return Optional.empty();
        }
        var state = states.findForUpdate(user.getId()).orElseGet(() -> new UserOtpState(user, Purpose.PASSWORD_RESET));
        state.usePurpose(Purpose.PASSWORD_RESET);
        Instant now = clock.instant();
        if (state.getOtpSentAt() != null && now.isBefore(state.getOtpSentAt().plus(COOLDOWN))) {
            return Optional.empty();
        }
        refreshWindow(state, now);
        if (state.getOtpSendCount() >= MAX_SENDS || state.getResetAttemptsInWindow() >= MAX_ACCOUNT_ATTEMPTS) {
            return Optional.empty();
        }

        String tag = credentialTag(user);
        if (!matches(state.getCredentialTag(), tag)) {
            state.clearChallenge();
        }
        String otp;
        String hash;
        do {
            otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            hash = otpHash(tag, otp);
        } while (hash.equals(state.getOtpCode()));
        // Keep any already-verified grant valid until it expires or the password changes.
        state.setCredentialTag(tag);
        state.setOtpCode(hash);
        state.setOtpExpiredAt(now.plus(OTP_TTL));
        state.setOtpSentAt(now);
        state.setOtpAttempts(0);
        state.setOtpSendCount(state.getOtpSendCount() + 1);
        states.save(state);
        return Optional.of(new ResetCode(user.getEmail(), otp));
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public ResetGrant verifyOtp(VerifyOtpRequest request) {
        User user = users.findByEmailForUpdate(request.email()).orElse(null);
        if (!eligible(user)) {
            throw invalidOtp();
        }
        var state = states.findForUpdate(user.getId())
                .filter(otp -> otp.isFor(Purpose.PASSWORD_RESET))
                .orElseThrow(PasswordResetService::invalidOtp);
        Instant now = clock.instant();
        String tag = credentialTag(user);
        if (state.getOtpCode() == null || !now.isBefore(state.getOtpExpiredAt())
                || !matches(state.getCredentialTag(), tag)) {
            throw invalidOtp();
        }
        refreshWindow(state, now);
        if (state.getOtpAttempts() >= MAX_OTP_ATTEMPTS || state.getResetAttemptsInWindow() >= MAX_ACCOUNT_ATTEMPTS) {
            throw invalidOtp();
        }
        state.setOtpAttempts(state.getOtpAttempts() + 1);
        state.setResetAttemptsInWindow(state.getResetAttemptsInWindow() + 1);
        if (!matches(state.getOtpCode(), otpHash(tag, request.otp()))) {
            throw invalidOtp();
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = now.plus(TOKEN_TTL);
        state.setOtpCode(null);
        state.setOtpExpiredAt(null);
        state.setResetTokenHash(hmac("token\n" + token));
        state.setResetTokenExpiresAt(expiresAt);
        return new ResetGrant(token, expiresAt);
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRMATION_MISMATCH",
                    "Xác nhận mật khẩu mới không khớp.");
        }
        String hash = hmac("token\n" + request.resetToken());
        Long userId = states.findUserIdByResetTokenHash(hash).orElseThrow(PasswordResetService::invalidToken);
        User user = users.findByIdForUpdate(userId).orElseThrow(PasswordResetService::invalidToken);
        var state = states.findForUpdate(userId).orElseThrow(PasswordResetService::invalidToken);
        if (!eligible(user) || !state.isFor(Purpose.PASSWORD_RESET) || !matches(state.getResetTokenHash(), hash)
                || !clock.instant().isBefore(state.getResetTokenExpiresAt())
                || !matches(state.getCredentialTag(), credentialTag(user))) {
            throw invalidToken();
        }
        user.setPassword(encoder.encode(request.newPassword()));
        state.clearChallenge();
        sessions.deleteByUserId(userId);
        return user.getEmail();
    }

    private boolean eligible(User user) {
        return user != null && user.getStatus() == UserStatus.ACTIVE && !googleIdentities.existsById(user.getId());
    }

    private void refreshWindow(UserOtpState state, Instant now) {
        if (state.getOtpWindowStartedAt() == null || !now.isBefore(state.getOtpWindowStartedAt().plus(RATE_WINDOW))) {
            state.setOtpWindowStartedAt(now);
            state.setOtpSendCount(0);
            state.setResetAttemptsInWindow(0);
        }
    }

    private String credentialTag(User user) {
        return hmac("account\n" + user.getId() + "\n" + user.getEmail() + "\n" + user.getPassword());
    }

    private String otpHash(String tag, String otp) {
        return hmac("otp\n" + tag + "\n" + otp);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(otpKey);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not compute password reset HMAC", ex);
        }
    }

    private static boolean matches(String stored, String calculated) {
        return stored != null && MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.US_ASCII), calculated.getBytes(StandardCharsets.US_ASCII));
    }

    private static InvalidOtpException invalidOtp() {
        return new InvalidOtpException("Mã OTP không hợp lệ, đã hết hạn hoặc đã hết lượt thử. Hãy kiểm tra mã hoặc yêu cầu mã mới.");
    }

    private static AuthException invalidToken() {
        return new AuthException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID",
                "Quyền đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng. Hãy yêu cầu mã mới.");
    }
}
