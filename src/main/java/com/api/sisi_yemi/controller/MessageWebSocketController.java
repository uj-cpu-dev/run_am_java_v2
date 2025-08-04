package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.SendMessageRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.service.MessageService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(MessageWebSocketController.class);

    private final MessageService messageService;
    private final AuthenticationHelper authHelper;

    @MessageMapping("/messages/{conversationId}/send")
    public void handleSendMessage(
            @DestinationVariable String conversationId,
            SendMessageRequest request) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            messageService.sendMessageHttp(conversationId, userId, request.getContent());
        } catch (ApiException e) {
            logger.error("Failed to send message via WebSocket: {}", e.getMessage());
            throw new MessagingException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending message via WebSocket", e);
            throw new MessagingException("Error sending message");
        }
    }

    @MessageMapping("/messages/{conversationId}/edit")
    public void handleEditMessage(
            @DestinationVariable String conversationId,
            EditMessageRequest request) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            messageService.editMessage(
                    conversationId,
                    request.getMessageId(),
                    request.getContent(),
                    userId
            );
        } catch (ApiException e) {
            logger.error("Failed to edit message via WebSocket: {}", e.getMessage());
            throw new MessagingException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error editing message via WebSocket", e);
            throw new MessagingException("Error editing message");
        }
    }

    @MessageMapping("/messages/{conversationId}/delete")
    public void handleDeleteMessage(
            @DestinationVariable String conversationId,
            DeleteMessageRequest request) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            messageService.deleteMessage(
                    conversationId,
                    request.getMessageId(),
                    userId
            );
        } catch (ApiException e) {
            logger.error("Failed to delete message via WebSocket: {}", e.getMessage());
            throw new MessagingException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting message via WebSocket", e);
            throw new MessagingException("Error deleting message");
        }
    }

    @MessageMapping("/messages/{conversationId}/mark-read")
    public void handleMarkAsRead(
            @DestinationVariable String conversationId) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            messageService.markMessagesAsRead(conversationId, userId);
        } catch (ApiException e) {
            logger.error("Failed to mark messages as read via WebSocket: {}", e.getMessage());
            throw new MessagingException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error marking messages as read via WebSocket", e);
            throw new MessagingException("Error marking messages as read");
        }
    }

    @SubscribeMapping("/messages/{conversationId}/initial-load")
    public List<MessageDto> handleInitialLoad(
            @DestinationVariable String conversationId) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            return messageService.getMessages(conversationId, userId);
        } catch (ApiException e) {
            logger.error("Failed to load initial messages via WebSocket: {}", e.getMessage());
            throw new MessagingException(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error loading initial messages via WebSocket", e);
            throw new MessagingException("Error loading messages");
        }
    }
}

@Data
class EditMessageRequest {
    private String messageId;
    private String content;
}

@Data
class DeleteMessageRequest {
    private String messageId;
}