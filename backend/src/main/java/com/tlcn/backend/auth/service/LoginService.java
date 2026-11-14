package com.tlcn.backend.auth.service;

import com.tlcn.backend.auth.dto.LoginRequest;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String dummyHash;
    public LoginService(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
        dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        User user = users.findByEmail(request.email()).orElse(null);
        boolean matches = encoder.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (user == null || !matches || user.getGoogleSubject() != null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng.");
        }
        requireActive(user);
        return user;
    }

    @Transactional(readOnly = true)
    public User loginGoogle(String subject, Boolean verified) {
        if (!Boolean.TRUE.equals(verified) || subject == null || subject.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_UNVERIFIED", "Google chưa xác minh tài khoản.");
        }
        User user = users.findByGoogleSubject(subject).orElseThrow(() ->
                new AuthException(HttpStatus.UNAUTHORIZED, "GOOGLE_NOT_REGISTERED", "Bạn cần đăng ký bằng Google trước khi đăng nhập."));
        requireActive(user);
        return user;
    }

    private void requireActive(User user) {
        if (user.getStatus() == UserStatus.PENDING_OTP) {
            throw new AuthException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Vui lòng xác minh email trước khi đăng nhập.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
    }
}
