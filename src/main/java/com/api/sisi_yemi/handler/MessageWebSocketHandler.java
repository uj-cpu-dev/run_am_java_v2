package com.api.sisi_yemi.handler;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.service.MessageService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final AuthenticationHelper authHelper;
    private final ObjectMapper objectMapper;
    private final MessageWebSocketHandler webSocketHandler;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        session.sendMessage(new TextMessage("✅ WebSocket connected"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();
            String conversationId = json.get("conversationId").asText();
            String userId = authHelper.getAuthenticatedUserId();

            switch (type) {
                case "send" -> {
                    String content = json.get("content").asText();
                    messageService.sendMessageHttp(conversationId, userId, content);
                    broadcast(conversationId, "newMessage", content);
                }

                case "edit" -> {
                    String messageId = json.get("messageId").asText();
                    String content = json.get("content").asText();
                    messageService.editMessage(conversationId, messageId, content, userId);
                    broadcast(conversationId, "messageEdited", messageId);
                }

                case "delete" -> {
                    String messageId = json.get("messageId").asText();
                    messageService.deleteMessage(conversationId, messageId, userId);
                    broadcast(conversationId, "messageDeleted", messageId);
                }

                case "mark-read" -> {
                    messageService.markMessagesAsRead(conversationId, userId);
                    // No need to broadcast
                }

                case "initial-load" -> {
                    List<MessageDto> messages = messageService.getMessages(conversationId, userId);
                    String payload = objectMapper.writeValueAsString(messages);
                    session.sendMessage(new TextMessage(payload));
                }

                default -> session.sendMessage(new TextMessage("❌ Unknown message type: " + type));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("❌ Error: " + e.getMessage()));
        }
    }

    public void broadcast(String conversationId, String eventType, String payload) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    Map<String, String> message = Map.of(
                            "type", eventType,
                            "conversationId", conversationId,
                            "data", payload
                    );
                    String json = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

}
