package com.tlcn.backend.user.service.serviceImpl;

import com.tlcn.backend.auth.repository.LoginSessionRepository;
import com.tlcn.backend.user.dto.*;
import com.tlcn.backend.user.entity.*;
import com.tlcn.backend.user.exception.ProfileException;
import com.tlcn.backend.user.repository.UserRepository;
import com.tlcn.backend.user.service.*;
import com.tlcn.backend.user.util.AvatarImage;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final LoginSessionRepository sessions;
    private final AvatarStorage avatars;

    public ProfileServiceImpl(UserRepository users, PasswordEncoder encoder,
            LoginSessionRepository sessions, AvatarStorage avatars) {
        this.users = users;
        this.encoder = encoder;
        this.sessions = sessions;
        this.avatars = avatars;
    }

    @Override @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) { return ProfileResponse.from(activeUser(userId, false)); }

    @Override @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = activeUser(userId, true);
        if (!(user instanceof Student) && (request.sex() != null || request.dateOfBirth() != null)) {
            throw new ProfileException(HttpStatus.BAD_REQUEST, "STUDENT_FIELDS_NOT_ALLOWED",
                    "Giới tính và ngày sinh chỉ áp dụng cho học sinh.");
        }
        if (user instanceof Student student && "OTHER".equals(request.sex()) && !"OTHER".equals(student.getSex())) {
            throw invalid("SEX_INVALID", "sex", "Vui lòng chọn giới tính Nam hoặc Nữ.");
        }
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setCity(request.city());
        if (user instanceof Student student) {
            student.setSex(request.sex());
            student.setDateOfBirth(request.dateOfBirth());
        }
        return ProfileResponse.from(users.save(user));
    }

    @Override @Transactional
    public ProfileResponse updateAvatar(Long userId, MultipartFile file) {
        User user = activeUser(userId, true);
        AvatarImage image = AvatarImage.read(file);
        String url = avatars.upload(userId, image.bytes(), image.contentType());
        user.setAvatarUrl(url);
        return ProfileResponse.from(users.save(user));
    }

    @Override @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = activeUser(userId, true);
        // Google accounts have a random placeholder hash and authenticate through Google only.
        if (user.getGoogleSubject() != null) {
            throw new ProfileException(HttpStatus.BAD_REQUEST, "GOOGLE_PASSWORD_MANAGED",
                    "Tài khoản Google cần đổi mật khẩu tại Google.");
        }
        if (!encoder.matches(request.currentPassword(), user.getPassword())) {
            throw invalid("CURRENT_PASSWORD_INVALID", "currentPassword", "Mật khẩu hiện tại không đúng.");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw invalid("PASSWORD_CONFIRMATION_MISMATCH", "confirmPassword", "Xác nhận mật khẩu mới không khớp.");
        }
        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw invalid("PASSWORD_UNCHANGED", "newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(encoder.encode(request.newPassword()));
        users.save(user);
        sessions.deleteByUserId(userId);
    }

    private User activeUser(Long userId, boolean lock) {
        User user = (lock ? users.findByIdForUpdate(userId) : users.findById(userId)).orElseThrow(() ->
                new ProfileException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Bạn cần đăng nhập."));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ProfileException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Tài khoản không hoạt động.");
        }
        return user;
    }

    private ProfileException invalid(String code, String field, String message) {
        return new ProfileException(HttpStatus.BAD_REQUEST, code, message, Map.of(field, message));
    }
}
