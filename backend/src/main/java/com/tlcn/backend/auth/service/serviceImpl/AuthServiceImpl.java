package com.tlcn.backend.auth.service.serviceImpl;

import com.tlcn.backend.auth.service.AuthService;
import com.tlcn.backend.auth.service.OtpDelivery;
import com.tlcn.backend.auth.exception.AuthException;
import com.tlcn.backend.auth.exception.InvalidOtpException;
import com.tlcn.backend.auth.entity.UserGoogleIdentity;
import com.tlcn.backend.auth.entity.UserOtpState;
import com.tlcn.backend.auth.repository.UserGoogleIdentityRepository;
import com.tlcn.backend.auth.repository.UserOtpStateRepository;

import com.tlcn.backend.auth.dto.*;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.UserRepository;
import com.tlcn.backend.user.repository.UserAccountDetailsRepository;
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
public class AuthServiceImpl implements AuthService {
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private final UserRepository users;
    private final UserAccountDetailsRepository accountDetails;
    private final UserOtpStateRepository otpStates;
    private final UserGoogleIdentityRepository googleIdentities;
    private final PasswordEncoder encoder;
    private final OtpDelivery delivery;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImpl(UserRepository users, UserAccountDetailsRepository accountDetails,
            UserOtpStateRepository otpStates, UserGoogleIdentityRepository googleIdentities,
            PasswordEncoder encoder, OtpDelivery delivery, Clock clock) {
        this.users = users;
        this.accountDetails = accountDetails;
        this.otpStates = otpStates;
        this.googleIdentities = googleIdentities;
        this.encoder = encoder;
        this.delivery = delivery;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        requirePublicRole(request.role());
        if (users.findByEmail(request.email()).isPresent()) {
            throw error(HttpStatus.CONFLICT, "EMAIL_EXISTS",
                    "Email đã được đăng ký. Nếu chưa xác minh, hãy chọn gửi lại OTP.");
        }
        User user = User.withRole(request.role());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(encoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setStatus(UserStatus.PENDING_OTP);
        users.saveAndFlush(user); // Enforce email uniqueness before sending mail.
        accountDetails.save(new UserAccountDetails(user, clock.instant()));
        UserOtpState otpState = new UserOtpState(user);
        issueOtp(user, otpState);
        return pending(user, otpState);
    }

    @Override
    @Transactional(noRollbackFor = InvalidOtpException.class)
    public RegistrationResponse verify(VerifyOtpRequest request) {
        User user = pendingUser(request.email());
        UserOtpState otpState = otpStates.findById(user.getId()).orElseThrow(() ->
                error(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Mã OTP đã hết hạn. Hãy yêu cầu mã mới."));
        Instant now = clock.instant();
        if (otpState.getOtpAttempts() >= 5) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_ATTEMPTS_EXCEEDED",
                    "Bạn đã nhập sai 5 lần. Hãy yêu cầu mã OTP mới.");
        }
        if (otpState.getOtpExpiredAt() == null || !now.isBefore(otpState.getOtpExpiredAt())) {
            throw error(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "Mã OTP đã hết hạn. Hãy yêu cầu mã mới.");
        }
        if (otpState.getOtpCode() == null || !encoder.matches(request.otp(), otpState.getOtpCode())) {
            otpState.setOtpAttempts(otpState.getOtpAttempts() + 1);
            throw new InvalidOtpException("Mã OTP không đúng. Vui lòng kiểm tra lại email.");
        }
        user.setStatus(UserStatus.ACTIVE);
        otpState.clearChallenge();
        return new RegistrationResponse(user.getEmail(), user.getStatus(), null, null,
                "Đăng ký thành công! Bạn có thể đăng nhập.");
    }

    @Override
    @Transactional
    public RegistrationResponse resend(EmailRequest request) {
        User user = pendingUser(request.email());
        UserOtpState otpState = otpStates.findById(user.getId()).orElseGet(() -> new UserOtpState(user));
        issueOtp(user, otpState);
        return pending(user, otpState);
    }

    @Override
    @Transactional
    public User registerGoogle(String subject, String email, Boolean verified, String name, Role role) {
        requirePublicRole(role);
        if (!Boolean.TRUE.equals(verified) || subject == null || subject.isBlank()
                || email == null || email.length() > 190) {
            throw error(HttpStatus.BAD_REQUEST, "GOOGLE_UNVERIFIED", "Google chưa xác minh email này.");
        }
        // UC01 only creates accounts; never implicitly link an existing email or change
        // its role/status.
        if (googleIdentities.existsBySubject(subject)
                || users.findByEmail(email.strip().toLowerCase(Locale.ROOT)).isPresent()) {
            throw error(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã có tài khoản. Hãy chuyển sang đăng nhập.");
        }
        User user = User.withRole(role);
        String fullName = name == null || name.isBlank() ? email : name.strip();
        user.setFullName(fullName.substring(0, Math.min(120, fullName.length())));
        user.setEmail(email.strip().toLowerCase(Locale.ROOT));
        // Google accounts cannot be authenticated using a guessed local password.
        user.setPassword(encoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        accountDetails.save(new UserAccountDetails(user, clock.instant()));
        googleIdentities.save(new UserGoogleIdentity(user, subject));
        return user;
    }

    @Override
    public void requirePublicRole(Role role) {
        if (role != Role.TEACHER && role != Role.STUDENT) {
            throw error(HttpStatus.BAD_REQUEST, "ROLE_INVALID", "Chỉ giáo viên và học sinh được tự đăng ký.");
        }
    }

    private User pendingUser(String email) {
        User user = users.findByEmailForUpdate(email).orElseThrow(() -> error(HttpStatus.BAD_REQUEST,
                "REGISTRATION_NOT_PENDING", "Không tìm thấy đăng ký đang chờ xác minh cho email này."));
        if (user.getStatus() != UserStatus.PENDING_OTP) {
            throw error(HttpStatus.CONFLICT, "REGISTRATION_NOT_PENDING", "Tài khoản không ở trạng thái chờ xác minh.");
        }
        return user;
    }

    private void issueOtp(User user, UserOtpState otpState) {
        Instant now = clock.instant();
        if (otpState.getOtpSentAt() != null && now.isBefore(otpState.getOtpSentAt().plus(COOLDOWN))) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_COOLDOWN", "Vui lòng đợi 60 giây giữa hai lần gửi OTP.");
        }
        if (otpState.getOtpWindowStartedAt() == null
                || !now.isBefore(otpState.getOtpWindowStartedAt().plus(Duration.ofHours(1)))) {
            otpState.setOtpWindowStartedAt(now);
            otpState.setOtpSendCount(0);
        }
        if (otpState.getOtpSendCount() >= 5) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_SEND_LIMIT",
                    "Đã đạt giới hạn 5 email xác minh mỗi giờ. Vui lòng thử lại sau.");
        }
        String otp;
        do {
            otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (otpState.getOtpCode() != null && encoder.matches(otp, otpState.getOtpCode()));
        otpState.setOtpCode(encoder.encode(otp));
        otpState.setOtpExpiredAt(now.plus(OTP_TTL));
        otpState.setOtpSentAt(now);
        otpState.setOtpAttempts(0);
        otpState.setOtpSendCount(otpState.getOtpSendCount() + 1);
        otpStates.saveAndFlush(otpState);
        delivery.send(user.getEmail(), otp);
    }

    private RegistrationResponse pending(User user, UserOtpState otpState) {
        return new RegistrationResponse(user.getEmail(), user.getStatus(), otpState.getOtpExpiredAt(),
                otpState.getOtpSentAt().plus(COOLDOWN), "Đã gửi mã xác minh đến email của bạn.");
    }

    private AuthException error(HttpStatus status, String code, String message) {
        return new AuthException(status, code, message);
    }
}
