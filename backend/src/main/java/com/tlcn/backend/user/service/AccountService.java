package com.tlcn.backend.user.service;

import com.tlcn.backend.auth.repository.LoginSessionRepository;
import com.tlcn.backend.user.dto.AccountRequests;
import com.tlcn.backend.user.dto.AccountResponse;
import com.tlcn.backend.user.entity.Role;
import com.tlcn.backend.user.entity.Student;
import com.tlcn.backend.user.entity.User;
import com.tlcn.backend.user.entity.UserAccountDetails;
import com.tlcn.backend.user.entity.UserStatus;
import com.tlcn.backend.user.exception.AccountException;
import com.tlcn.backend.user.repository.UserAccountDetailsRepository;
import com.tlcn.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserRepository users;
    private final UserAccountDetailsRepository accountDetails;
    private final LoginSessionRepository sessions;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AccountService(UserRepository users, UserAccountDetailsRepository accountDetails,
            LoginSessionRepository sessions, PasswordEncoder passwords, JdbcTemplate jdbc, Clock clock) {
        this.users = users;
        this.accountDetails = accountDetails;
        this.sessions = sessions;
        this.passwords = passwords;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountResponse.Listing list(String keyword, Role role, UserStatus status, int page, int size) {
        String term = keyword.strip().toLowerCase(Locale.ROOT);
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Specification<User> filters = (user, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (!term.isEmpty()) {
                predicates.add(builder.or(
                        builder.like(builder.lower(user.get("fullName")), pattern, '!'),
                        builder.like(builder.lower(user.get("email")), pattern, '!')));
            }
            if (role != null) predicates.add(builder.equal(user.get("role"), role));
            if (status != null) predicates.add(builder.equal(user.get("status"), status));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var result = users.findAll(filters, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var detailsById = accountDetails.findAllById(result.map(User::getId).getContent()).stream()
                .collect(Collectors.toMap(UserAccountDetails::getUserId, Function.identity()));
        return AccountResponse.Listing.from(result.map(user -> response(user, detailsById.get(user.getId()))));
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        User user = users.findById(id).orElseThrow(AccountService::notFound);
        return response(user, accountDetails.findById(id).orElse(null));
    }

    @Transactional
    public AccountResponse create(Long adminId, AccountRequests.Create request) {
        String password = passwords.encode(request.password());
        requireAdmin(lock(adminId));
        User user = User.withRole(request.role());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPassword(password);
        save(user);
        return response(user, accountDetails.save(new UserAccountDetails(user, clock.instant())));
    }

    @Transactional
    public AccountResponse update(Long adminId, Long id, AccountRequests.Update request) {
        User user = lockForAdmin(adminId, id);
        UserAccountDetails details = detailsFor(user);
        Role previousRole = user.getRole();
        boolean roleChanged = previousRole != request.role();
        boolean emailChanged = !user.getEmail().equals(request.email());
        if (roleChanged && adminId.equals(id)) {
            throw new AccountException(HttpStatus.CONFLICT, "SELF_ROLE_CHANGE", "Bạn không thể tự đổi vai trò quản trị.");
        }
        if (roleChanged && user instanceof Student student) {
            if (student.getClassroom() != null) throw roleInUse();
            details.rememberStudentDetails(student.getSex(), student.getDateOfBirth());
        }

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setRole(request.role());
        if (emailChanged && (user.getStatus() == UserStatus.PENDING_OTP
                || details.getStatusBeforeLock() == UserStatus.PENDING_OTP)) {
            // A code delivered to the old address must never verify the new address.
            user.setOtpCode(null);
            user.setOtpExpiredAt(null);
            user.setOtpSentAt(null);
            user.setOtpWindowStartedAt(null);
            user.setOtpSendCount(0);
            user.setOtpAttempts(0);
        }
        save(user);
        if (roleChanged) {
            changeRoleProfile(user, previousRole, details);
            accountDetails.save(details);
        }
        if (roleChanged || emailChanged) sessions.deleteByUserId(id);
        return response(user, details);
    }

    @Transactional
    public AccountResponse setLocked(Long adminId, Long id, boolean locked) {
        User user = lockForAdmin(adminId, id);
        UserAccountDetails details = detailsFor(user);
        boolean currentlyLocked = user.getStatus() == UserStatus.LOCKED;
        if (locked == currentlyLocked) return response(user, details);
        if (locked) {
            if (adminId.equals(id)) {
                throw new AccountException(HttpStatus.CONFLICT, "SELF_LOCK", "Bạn không thể tự khóa tài khoản.");
            }
            details.setStatusBeforeLock(user.getStatus());
            user.setStatus(UserStatus.LOCKED);
            accountDetails.save(details);
            sessions.deleteByUserId(id);
        } else {
            user.setStatus(details.getStatusBeforeLock() == UserStatus.PENDING_OTP
                    ? UserStatus.PENDING_OTP : UserStatus.ACTIVE);
            details.setStatusBeforeLock(null);
        }
        return response(user, details);
    }

    private UserAccountDetails detailsFor(User user) {
        return accountDetails.findById(user.getId()).orElseGet(() -> new UserAccountDetails(user, null));
    }

    private static AccountResponse response(User user, UserAccountDetails details) {
        return AccountResponse.from(user, details == null ? null : details.getCreatedAt());
    }

    private void changeRoleProfile(User user, Role previousRole, UserAccountDetails details) {
        // Joined inheritance maps Teacher and Student to their own rows with the same user ID.
        // The foreign keys protect classrooms, assessments and student history from being orphaned.
        try {
            if (previousRole == Role.STUDENT) {
                jdbc.update("delete from students where id = ?", user.getId());
            } else if (previousRole == Role.TEACHER) {
                jdbc.update("delete from teachers where id = ?", user.getId());
            }
            if (user.getRole() == Role.STUDENT) {
                jdbc.update("insert into students (id, sex, date_of_birth) values (?, ?, ?)",
                        user.getId(), details.getFormerStudentSex(), details.getFormerStudentDateOfBirth());
                details.rememberStudentDetails(null, null);
            } else if (user.getRole() == Role.TEACHER) {
                jdbc.update("insert into teachers (id) values (?)", user.getId());
            }
        } catch (DataIntegrityViolationException ex) {
            throw roleInUse();
        }
    }

    private User save(User user) {
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new AccountException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email đã được sử dụng.");
        }
    }

    private User lockForAdmin(Long adminId, Long id) {
        // Lock in ID order so two administrators cannot concurrently remove each other's access.
        if (adminId.compareTo(id) <= 0) {
            User admin = lock(adminId);
            requireAdmin(admin);
            return adminId.equals(id) ? admin : lock(id);
        }
        User target = lock(id);
        requireAdmin(lock(adminId));
        return target;
    }

    private User lock(Long id) {
        return users.findByIdForUpdate(id).orElseThrow(AccountService::notFound);
    }

    private static void requireAdmin(User user) {
        if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Bạn cần quyền quản trị.");
        }
    }

    private static AccountException notFound() {
        return new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản.");
    }

    private static AccountException roleInUse() {
        return new AccountException(HttpStatus.CONFLICT, "ROLE_IN_USE",
                "Không thể đổi vai trò khi tài khoản còn liên kết với lớp học hoặc dữ liệu học tập.");
    }
}
