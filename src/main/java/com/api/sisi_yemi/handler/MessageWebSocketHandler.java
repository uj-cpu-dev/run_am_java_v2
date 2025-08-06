package com.api.sisi_yemi.handler;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.service.MessageService;
import com.api.sisi_yemi.util.token.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA.withReason("URI not provided."));
            return;
        }

        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        if (token == null || token.isEmpty()) {
            session.close(CloseStatus.TLS_HANDSHAKE_FAILURE.withReason("Authentication token not provided."));
            return;
        }

        String userId;
        try {
            // This is where you would use the token to get the user ID
            userId = jwtTokenProvider.getUserIdFromToken(token);
        } catch (Exception e) {
            session.close(CloseStatus.TLS_HANDSHAKE_FAILURE.withReason("Invalid or expired token."));
            return;
        }

        sessions.put(session.getId(), session);
        userSessions.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(session);

        session.getAttributes().put("userId", userId);
        session.sendMessage(new TextMessage("✅ WebSocket connected as user: " + userId));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.TLS_HANDSHAKE_FAILURE.withReason("User not authenticated."));
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();
            String conversationId = json.get("conversationId").asText();
            // Use the userId from the session's attributes
            // String userId = authHelper.getAuthenticatedUserId();

            switch (type) {
                case "send" -> {
                    String content = json.get("content").asText();
                    MessageDto sentMessage = messageService.sendMessageHttp(conversationId, userId, content);
                    broadcastMessage(conversationId, "new", sentMessage);
                }
                case "edit" -> {
                    String messageId = json.get("messageId").asText();
                    String content = json.get("content").asText();
                    MessageDto editedMessage = messageService.editMessage(conversationId, messageId, content, userId);
                    broadcastMessage(conversationId, "edit", editedMessage);
                }
                case "delete" -> {
                    String messageId = json.get("messageId").asText();
                    messageService.deleteMessage(conversationId, messageId, userId);
                    broadcastDelete(conversationId, messageId);
                }
                case "mark-read" -> {
                    messageService.markMessagesAsRead(conversationId, userId);
                }
                case "initial-load" -> {
                    List<MessageDto> messages = messageService.getMessages(conversationId, userId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messages)));
                }
                default -> session.sendMessage(new TextMessage("❌ Unknown message type: " + type));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("❌ Error: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());

        userSessions.forEach((userId, sessionList) ->
                sessionList.removeIf(s -> s.getId().equals(session.getId()))
        );
    }

    public void sendMessageToUser(String userId, Object payload) {
        List<WebSocketSession> userSessionList = userSessions.get(userId);
        if (userSessionList == null) return;

        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : userSessionList) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("WebSocket send error", e);
        }
    }

    private void broadcastMessage(String conversationId, String action, MessageDto message) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("message", message);

            List<String> userIds = messageService.getParticipantUserIds(conversationId);
            for (String userId : userIds) {
                sendMessageToUser(userId, payload);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast {} message: {}", action, message.getId(), e);
        }
    }

    private void broadcastDelete(String conversationId, String messageId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "delete");
            payload.put("messageId", messageId);
            payload.put("conversationId", conversationId);

            List<String> userIds = messageService.getParticipantUserIds(conversationId);
            for (String userId : userIds) {
                sendMessageToUser(userId, payload);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast message deletion: {}", messageId, e);
        }
    }
}
