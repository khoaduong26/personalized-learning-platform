package com.tlcn.backend.notification;

import com.tlcn.backend.user.dto.UserSummary;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final JsonMapper mapper;

    public NotificationWebSocketHandler(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (session.getPrincipal() instanceof UserSummary user) {
            sessions.computeIfAbsent(user.id(), ignored -> new CopyOnWriteArraySet<>()).add(session);
            return;
        }
        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authenticated user required"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("Notification socket closed after transport error.");
        removeSession(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void sendToUser(Long userId, NotificationEvent event) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }

        final String payload;
        try {
            payload = mapper.writeValueAsString(event);
        } catch (Exception exception) {
            log.error("Unable to serialize notification event type={}", event.type(), exception);
            return;
        }

        for (WebSocketSession session : userSessions) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                    } else {
                        removeSession(session);
                    }
                }
            } catch (IOException exception) {
                log.debug("Could not deliver notification over WebSocket.");
                removeSession(session);
            }
        }
    }

    private void removeSession(WebSocketSession session) {
        if (!(session.getPrincipal() instanceof UserSummary user)) {
            return;
        }
        Set<WebSocketSession> userSessions = sessions.get(user.id());
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sessions.remove(user.id(), userSessions);
            }
        }
    }
}
