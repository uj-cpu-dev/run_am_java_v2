package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.UserDto;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.model.Message;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
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

    private final SimpMessagingTemplate messagingTemplate;


    public MessageDto sendMessageHttp(String conversationId, String senderId, String content) {
        // Validate input
        validateMessageInput(conversationId, senderId, content);

        // Get required tables
        DynamoDbHelper<Message> msgTable = dynamoDbUtilHelper.getMessageTable();
        DynamoDbHelper<Conversation> convTable = dynamoDbUtilHelper.getConversationTable();
        DynamoDbHelper<User> userTable = dynamoDbUtilHelper.getUserTable();

        // Retrieve conversation and validate access
        Conversation conversation = getConversationWithValidation(convTable, conversationId, senderId);
        User sender = getUserWithValidation(userTable, senderId);

        // Create and save message
        Message message = createMessage(conversationId, senderId, content);
        msgTable.save(message);

        // Update conversation
        updateConversationAfterMessage(convTable, conversation, content, senderId);

        // Notify via WebSocket
        notifyViaWebSocket(conversationId, message, sender);

        return convertToDto(message, sender);
    }

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
                .readBy(null)
                .build();
    }

    private void updateConversationAfterMessage(DynamoDbHelper<Conversation> convTable,
                                                Conversation conversation,
                                                String content,
                                                String senderId) {
        String receiverId = senderId.equals(conversation.getParticipantId())
                ? conversation.getSellerId()
                : conversation.getParticipantId();

        conversation.setLastMessage(content);
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

    private void notifyViaWebSocket(String conversationId, Message message, User sender) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/messages/" + conversationId,
                    convertToDto(message, sender)
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for message {}", message.getMessageId(), e);
        }
    }

    public List<MessageDto> getMessages(String conversationId, String userId) {
        DynamoDbHelper<Message> msgTable = dynamoDbUtilHelper.getMessageTable();
        DynamoDbHelper<Conversation> convTable = dynamoDbUtilHelper.getConversationTable();
        DynamoDbHelper<User> userTable = dynamoDbUtilHelper.getUserTable();

        Conversation conversation = convTable.getById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        if (!List.of(conversation.getParticipantId(), conversation.getSellerId()).contains(userId)) {
            throw new ApiException("Access denied", FORBIDDEN, "ACCESS_DENIED");
        }

        return msgTable.findAll().stream()
                .filter(msg -> msg.getConversationId().equals(conversationId))
                .sorted(Comparator.comparing(Message::getTimestamp))
                .map(msg -> {
                    User sender = userTable.getById(msg.getSenderId()).orElse(null);
                    return convertToDto(msg, sender);
                })
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(String conversationId, String userId) {
        DynamoDbHelper<Message> msgTable = dynamoDbUtilHelper.getMessageTable();
        DynamoDbHelper<Conversation> convTable = dynamoDbUtilHelper.getConversationTable();

        Conversation conversation = convTable.getById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        if (!List.of(conversation.getParticipantId(), conversation.getSellerId()).contains(userId)) {
            throw new ApiException("Access denied", FORBIDDEN, "ACCESS_DENIED");
        }

        List<Message> unreadMessages = msgTable.findAll().stream()
                .filter(msg -> msg.getConversationId().equals(conversationId))
                .filter(msg -> !msg.getSenderId().equals(userId) && msg.isReadBy(userId))
                .toList();

        // Mark messages as read by this user
        for (Message msg : unreadMessages) {
            msg.markReadBy(userId);
            msgTable.save(msg);
        }

        updateUnreadCount(conversation, userId);
    }

    private void updateUnreadCount(Conversation conversation, String userId) {
        DynamoDbHelper<Message> msgTable = dynamoDbUtilHelper.getMessageTable();

        long unreadCount = msgTable.findAll().stream()
                .filter(msg -> msg.getConversationId().equals(conversation.getId()))
                .filter(msg -> !msg.getSenderId().equals(userId) && msg.isReadBy(userId))
                .count();

        // Update conversation based on who is viewing
        if (userId.equals(conversation.getParticipantId())) {
            conversation.setParticipantUnread((int) unreadCount);
        } else {
            conversation.setSellerUnread((int) unreadCount);
        }

        dynamoDbUtilHelper.getConversationTable().save(conversation);
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

