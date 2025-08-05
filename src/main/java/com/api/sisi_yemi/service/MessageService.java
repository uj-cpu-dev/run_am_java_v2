package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.UserDto;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.handler.MessageWebSocketHandler;
import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.model.Message;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final DynamoDbUtilHelper dynamoDbUtilHelper;
    private final MessageWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public MessageDto sendMessageHttp(String conversationId, String senderId, String content) {
        validateMessageInput(conversationId, senderId, content);

        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var convTable = dynamoDbUtilHelper.getConversationTable();
        var userTable = dynamoDbUtilHelper.getUserTable();

        Conversation conversation = getConversationWithValidation(convTable, conversationId, senderId);
        User sender = getUserWithValidation(userTable, senderId);

        Message message = createMessage(conversationId, senderId, content);
        msgTable.save(message);

        updateConversationAfterMessage(convTable, conversation, content, senderId, message.getMessageId());

        notifyViaWebSocket(conversationId, convertToDto(message, sender), "new");

        return convertToDto(message, sender);
    }

    public List<MessageDto> getMessages(String conversationId, String userId) {
        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var convTable = dynamoDbUtilHelper.getConversationTable();
        var userTable = dynamoDbUtilHelper.getUserTable();

        Conversation conversation = getConversationWithValidation(convTable, conversationId, userId);

        return msgTable.getByPartitionKey(conversationId).stream()
                .filter(msg -> !msg.isDeleted())
                .sorted(Comparator.comparing(Message::getTimestamp))
                .map(msg -> convertToDto(msg, userTable.getById(msg.getSenderId()).orElse(null)))
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(String conversationId, String userId) {
        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var convTable = dynamoDbUtilHelper.getConversationTable();

        Conversation conversation = getConversationWithValidation(convTable, conversationId, userId);

        List<Message> unreadMessages = msgTable.getByPartitionKey(conversationId).stream()
                .filter(msg -> !msg.isDeleted())
                .filter(msg -> !msg.getSenderId().equals(userId) && !msg.isReadBy(userId))
                .toList();

        unreadMessages.forEach(msg -> {
            msg.markReadBy(userId);
            msgTable.save(msg);
        });

        updateUnreadCount(conversation, userId);
    }

    public MessageDto editMessage(String conversationId, String messageId, String content, String userId) {
        if (content == null || content.trim().isEmpty()) {
            throw new ApiException("Message content cannot be empty", BAD_REQUEST, "EMPTY_MESSAGE");
        }

        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var userTable = dynamoDbUtilHelper.getUserTable();

        Message message = msgTable.getByPartitionKey(conversationId).stream()
                .filter(m -> m.getMessageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Message not found", NOT_FOUND, "MESSAGE_NOT_FOUND"));

        if (!message.getSenderId().equals(userId)) {
            throw new ApiException("You can only edit your own messages", FORBIDDEN, "EDIT_NOT_ALLOWED");
        }

        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        msgTable.save(message);

        User sender = getUserWithValidation(userTable, userId);

        notifyViaWebSocket(conversationId, convertToDto(message, sender), "edit");

        return convertToDto(message, sender);
    }

    public void deleteMessage(String conversationId, String messageId, String userId) {
        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var convTable = dynamoDbUtilHelper.getConversationTable();

        Message message = msgTable.getByPartitionKey(conversationId).stream()
                .filter(m -> m.getMessageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Message not found", NOT_FOUND, "MESSAGE_NOT_FOUND"));

        if (!message.getSenderId().equals(userId)) {
            throw new ApiException("You can only delete your own messages", FORBIDDEN, "DELETE_NOT_ALLOWED");
        }

        Conversation conversation = getConversationWithValidation(convTable, conversationId, userId);

        message.softDelete();
        msgTable.save(message);

        if (messageId.equals(conversation.getLastMessageId())) {
            updateConversationLastMessage(conversation, msgTable);
        }

        notifyMessageDeletion(conversationId, messageId);
    }

    // ========== Private Helpers ==========

    private void validateMessageInput(String conversationId, String senderId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ApiException("Message content cannot be empty", BAD_REQUEST, "EMPTY_MESSAGE");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new ApiException("Conversation ID cannot be empty", BAD_REQUEST, "EMPTY_CONVERSATION_ID");
        }
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new ApiException("Sender ID cannot be empty", BAD_REQUEST, "EMPTY_SENDER_ID");
        }
    }

    private Conversation getConversationWithValidation(DynamoDbHelper<Conversation> convTable,
                                                       String conversationId, String senderId) {
        Conversation conversation = convTable.getById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        if (!List.of(conversation.getParticipantId(), conversation.getSellerId()).contains(senderId)) {
            throw new ApiException("Access denied", FORBIDDEN, "ACCESS_DENIED");
        }

        return conversation;
    }

    private User getUserWithValidation(DynamoDbHelper<User> userTable, String userId) {
        return userTable.getById(userId)
                .orElseThrow(() -> new ApiException("User not found", NOT_FOUND, "USER_NOT_FOUND"));
    }

    private Message createMessage(String conversationId, String senderId, String content) {
        return Message.builder()
                .conversationId(conversationId)
                .messageId(UUID.randomUUID().toString())
                .senderId(senderId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .status("delivered")
                .build();
    }

    private void updateConversationAfterMessage(DynamoDbHelper<Conversation> convTable,
                                                Conversation conversation,
                                                String content,
                                                String senderId,
                                                String messageId) {
        String receiverId = senderId.equals(conversation.getParticipantId())
                ? conversation.getSellerId()
                : conversation.getParticipantId();

        conversation.setLastMessage(content);
        conversation.setLastMessageId(messageId);
        conversation.setTimestamp(LocalDateTime.now());

        if (!senderId.equals(receiverId)) {
            if (senderId.equals(conversation.getParticipantId())) {
                conversation.setSellerUnread(conversation.getSellerUnread() + 1);
            } else {
                conversation.setParticipantUnread(conversation.getParticipantUnread() + 1);
            }
        }

        convTable.save(conversation);
    }

    private void updateConversationLastMessage(Conversation conversation, DynamoDbHelper<Message> msgTable) {
        List<Message> messages = msgTable.getByPartitionKey(conversation.getId()).stream()
                .filter(msg -> !msg.isDeleted())
                .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                .toList();

        if (!messages.isEmpty()) {
            Message last = messages.get(0);
            conversation.setLastMessage(last.getContent());
            conversation.setLastMessageId(last.getMessageId());
            conversation.setTimestamp(last.getTimestamp());
        } else {
            conversation.setLastMessage(null);
            conversation.setLastMessageId(null);
            conversation.setTimestamp(LocalDateTime.now());
        }

        dynamoDbUtilHelper.getConversationTable().save(conversation);
    }

    private void updateUnreadCount(Conversation conversation, String userId) {
        var msgTable = dynamoDbUtilHelper.getMessageTable();

        long unreadCount = msgTable.getByPartitionKey(conversation.getId()).stream()
                .filter(msg -> !msg.isDeleted())
                .filter(msg -> !msg.getSenderId().equals(userId) && !msg.isReadBy(userId))
                .count();

        if (userId.equals(conversation.getParticipantId())) {
            conversation.setParticipantUnread((int) unreadCount);
        } else {
            conversation.setSellerUnread((int) unreadCount);
        }

        dynamoDbUtilHelper.getConversationTable().save(conversation);
    }

    private void notifyViaWebSocket(String conversationId, MessageDto message, String action) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", action);
            payload.put("message", message);
            webSocketHandler.broadcast(
                    conversationId,
                    "message-event",
                    objectMapper.writeValueAsString(payload)
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for message {}", message.getId(), e);
        }
    }

    private void notifyMessageDeletion(String conversationId, String messageId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "delete");
            payload.put("messageId", messageId);
            payload.put("conversationId", conversationId);
            webSocketHandler.broadcast(
                    conversationId,
                    "message-event",
                    objectMapper.writeValueAsString(payload)
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for deleted message {}", messageId, e);
        }
    }

    private MessageDto convertToDto(Message message, User sender) {
        var dto = new MessageDto();
        dto.setId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setTimestamp(message.getTimestamp());
        dto.setSender(convertUserDto(sender));
        return dto;
    }

    private UserDto convertUserDto(User user) {
        var dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setAvatar(user.getAvatarUrl());
        dto.setVerified(user.isEmailVerified());
        dto.setRating(user.getRating());
        return dto;
    }
}