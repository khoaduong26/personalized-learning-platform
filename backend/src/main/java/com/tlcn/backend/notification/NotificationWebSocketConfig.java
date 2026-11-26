package com.tlcn.backend.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler handler;
    private final String frontendUrl;

    public NotificationWebSocketConfig(NotificationWebSocketHandler handler,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.handler = handler;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notifications").setAllowedOrigins(frontendUrl);
    }
}
