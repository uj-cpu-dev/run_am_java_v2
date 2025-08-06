package com.api.sisi_yemi.config;


import com.api.sisi_yemi.handler.MessageWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;

    public WebSocketConfig(MessageWebSocketHandler myWebSocketHandler) {
        this.messageWebSocketHandler = myWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(messageWebSocketHandler, "/ws-messages")
                .setAllowedOrigins("http://localhost:8081", "https://runam.africa");
    }
}
