package com.tlcn.backend.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(String id, String type, String title, String message, String href,
        Instant createdAt) {
    public static NotificationEvent create(String type, String title, String message, String href) {
        return new NotificationEvent(UUID.randomUUID().toString(), type, title, message, href, Instant.now());
    }
}
