package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, PasswordEncoder encoder, OtpDelivery delivery, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        requirePublicRole(request.role());
        if (users.findByEmail(request.email()).isPresent()) {
            throw error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được đăng ký. Nếu chưa xác minh, hãy chọn gửi lại OTP.");
        }
        User user = newUser(request.role());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(encoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setStatus(UserStatus.PENDING_OTP);
        users.saveAndFlush(user); // Enforce email uniqueness before sending mail.
        issueOtp(user);
        return pending(user);
    }

    @Transactional(noRollbackFor = InvalidOtpException.class)
    public RegistrationResponse verify(VerifyOtpRequest request) {
        User user = pendingUser(request.email());
        Instant now = clock.instant();
        if (user.getOtpAttempts() >= 5) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_ATTEMPTS_EXCEEDED", "Bạn đã nhập sai 5 lần. Hãy yêu cầu mã OTP mới.");
        }
        if (user.getOtpExpiredAt() == null || !now.isBefore(user.getOtpExpiredAt())) {
            throw error(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Mã OTP đã hết hạn. Hãy yêu cầu mã mới.");
        }
        if (user.getOtpCode() == null || !encoder.matches(request.otp(), user.getOtpCode())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            throw new InvalidOtpException("Mã OTP không đúng. Vui lòng kiểm tra lại email.");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        user.setOtpAttempts(0);
        return new RegistrationResponse(user.getEmail(), user.getStatus(), null, null,
                "Đăng ký thành công! Bạn có thể đăng nhập.");
    }

    @Transactional
    public RegistrationResponse resend(EmailRequest request) {
        User user = pendingUser(request.email());
        issueOtp(user);
        return pending(user);
    }

    @Transactional
    public User registerGoogle(String subject, String email, Boolean verified, String name, Role role) {
        requirePublicRole(role);
        if (!Boolean.TRUE.equals(verified) || subject == null || subject.isBlank()
                || email == null || email.length() > 190) {
            throw error(HttpStatus.BAD_REQUEST, "GOOGLE_UNVERIFIED", "Google chưa xác minh email này.");
        }
        // UC01 only creates accounts; never implicitly link an existing email or change its role/status.
        if (users.findByGoogleSubject(subject).isPresent()
                || users.findByEmail(email.strip().toLowerCase(Locale.ROOT)).isPresent()) {
            throw error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã có tài khoản. Hãy chuyển sang đăng nhập.");
        }
        User user = newUser(role);
        String fullName = name == null || name.isBlank() ? email : name.strip();
        user.setFullName(fullName.substring(0, Math.min(120, fullName.length())));
        user.setEmail(email.strip().toLowerCase(Locale.ROOT));
        user.setGoogleSubject(subject);
        // Google accounts cannot be authenticated using a guessed local password.
        user.setPassword(encoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    public void requirePublicRole(Role role) {
        if (role != Role.TEACHER && role != Role.STUDENT) {
            throw error(HttpStatus.BAD_REQUEST, "ROLE_INVALID", "Chỉ giáo viên và học sinh được tự đăng ký.");
        }
    }

    private User newUser(Role role) { return role == Role.TEACHER ? new Teacher() : new Student(); }

    private User pendingUser(String email) {
        User user = users.findByEmailForUpdate(email).orElseThrow(() ->
                error(HttpStatus.BAD_REQUEST, "REGISTRATION_NOT_PENDING", "Không tìm thấy đăng ký đang chờ xác minh cho email này."));
        if (user.getStatus() != UserStatus.PENDING_OTP) {
            throw error(HttpStatus.CONFLICT, "REGISTRATION_NOT_PENDING", "Tài khoản không ở trạng thái chờ xác minh.");
        }
        return user;
    }

    private void issueOtp(User user) {
        Instant now = clock.instant();
        if (user.getOtpSentAt() != null && now.isBefore(user.getOtpSentAt().plus(COOLDOWN))) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN", "Vui lòng đợi 60 giây giữa hai lần gửi OTP.");
        }
        if (user.getOtpWindowStartedAt() == null
                || !now.isBefore(user.getOtpWindowStartedAt().plus(Duration.ofHours(1)))) {
            user.setOtpWindowStartedAt(now);
            user.setOtpSendCount(0);
        }
        if (user.getOtpSendCount() >= 5) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_SEND_LIMIT", "Đã đạt giới hạn 5 email xác minh mỗi giờ. Vui lòng thử lại sau.");
        }
        String otp;
        do {
            otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (user.getOtpCode() != null && encoder.matches(otp, user.getOtpCode()));
        user.setOtpCode(encoder.encode(otp));
        user.setOtpExpiredAt(now.plus(OTP_TTL));
        user.setOtpSentAt(now);
        user.setOtpAttempts(0);
        user.setOtpSendCount(user.getOtpSendCount() + 1);
        delivery.send(user.getEmail(), otp);
    }

    private RegistrationResponse pending(User user) {
        return new RegistrationResponse(user.getEmail(), user.getStatus(), user.getOtpExpiredAt(),
                user.getOtpSentAt().plus(COOLDOWN), "Đã gửi mã xác minh đến email của bạn.");
    }

    private AuthException error(HttpStatus status, String code, String message) {
        return new AuthException(status, code, message);
    }
}
