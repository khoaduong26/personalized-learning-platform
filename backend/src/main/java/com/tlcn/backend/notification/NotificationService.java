package com.tlcn.backend.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {
    private final NotificationWebSocketHandler handler;

    public NotificationService(NotificationWebSocketHandler handler) {
        this.handler = handler;
    }

    public void notifyAfterCommit(Long userId, String type, String title, String message, String href) {
        NotificationEvent event = NotificationEvent.create(type, title, message, href);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            handler.sendToUser(userId, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                handler.sendToUser(userId, event);
            }
        });
    }
}
