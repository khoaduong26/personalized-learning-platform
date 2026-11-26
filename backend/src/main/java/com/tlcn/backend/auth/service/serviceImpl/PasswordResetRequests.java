package com.tlcn.backend.auth.service.serviceImpl;

import com.tlcn.backend.auth.exception.AuthException;
import com.tlcn.backend.auth.service.OtpDelivery;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetRequests {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetRequests.class);
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final int MAX_IPS = 10_000;

    private final PasswordResetService resets;
    private final OtpDelivery delivery;
    private final Clock clock;
    private final Map<String, IpWindow> ipWindows = new HashMap<>();
    private Instant nextCleanup;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(64), Thread.ofPlatform().daemon().name("password-reset-", 0).factory());

    public PasswordResetRequests(PasswordResetService resets, OtpDelivery delivery, Clock clock) {
        this.resets = resets;
        this.delivery = delivery;
        this.clock = clock;
    }

    public void requestOtp(String email, String ip) {
        limit(ip, true);
        Instant requestedAt = clock.instant();
        try {
            executor.execute(() -> {
                try {
                    if (clock.instant().isBefore(requestedAt.plusSeconds(60))) {
                        resets.issueOtp(email).ifPresent(code -> delivery.sendPasswordResetCode(code.email(), code.otp()));
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("Password reset code could not be delivered ({})", ex.getClass().getSimpleName());
                }
            });
        } catch (RejectedExecutionException ex) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "RESET_BUSY", "Vui lòng thử lại sau.");
        }
    }

    public void limitVerification(String ip) {
        limit(ip, false);
    }

    public void notifyPasswordChanged(String email) {
        try {
            executor.execute(() -> {
                try {
                    delivery.sendPasswordResetNotice(email);
                } catch (RuntimeException ex) {
                    LOG.warn("Password reset notice could not be delivered ({})", ex.getClass().getSimpleName());
                }
            });
        } catch (RejectedExecutionException ex) {
            LOG.warn("Password reset notice could not be queued");
        }
    }

    private synchronized void limit(String ip, boolean sending) {
        Instant now = clock.instant();
        if (nextCleanup == null || !now.isBefore(nextCleanup)) {
            ipWindows.values().removeIf(window -> !now.isBefore(window.expiresAt));
            nextCleanup = now.plusSeconds(60);
        }
        IpWindow window = ipWindows.get(ip);
        if (window == null || !now.isBefore(window.expiresAt)) {
            if (window == null && ipWindows.size() >= MAX_IPS) {
                throw rateLimited();
            }
            window = new IpWindow(now.plus(WINDOW));
            ipWindows.put(ip, window);
        }
        if (sending ? ++window.sends > 30 : ++window.verifications > 100) {
            throw rateLimited();
        }
    }

    private static AuthException rateLimited() {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "RESET_RATE_LIMIT",
                "Đã có quá nhiều yêu cầu. Vui lòng thử lại sau.");
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class IpWindow {
        private final Instant expiresAt;
        private int sends;
        private int verifications;

        private IpWindow(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
