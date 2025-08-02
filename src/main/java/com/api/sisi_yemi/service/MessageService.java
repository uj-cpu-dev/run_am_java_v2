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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final DynamoDbUtilHelper dynamoDbUtilHelper;

    private final SimpMessagingTemplate messagingTemplate;


    public MessageDto sendMessageHttp(String conversationId, String senderId, String content) {
        DynamoDbHelper<Message> msgTable = dynamoDbUtilHelper.getMessageTable();
        DynamoDbHelper<Conversation> convTable = dynamoDbUtilHelper.getConversationTable();
        DynamoDbHelper<User> userTable = dynamoDbUtilHelper.getUserTable();

        Conversation conversation = convTable.getById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        String receiverId = senderId.equals(conversation.getParticipantId())
                ? conversation.getSellerId()
                : conversation.getParticipantId();

        if (!List.of(conversation.getParticipantId(), conversation.getSellerId()).contains(senderId)) {
            throw new ApiException("Access denied", FORBIDDEN, "ACCESS_DENIED");
        }

        User sender = userTable.getById(senderId)
                .orElseThrow(() -> new ApiException("User not found", NOT_FOUND, "USER_NOT_FOUND"));

        Message message = Message.builder()
                .conversationId(conversationId)
                .messageId(UUID.randomUUID().toString())
                .senderId(senderId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .status("delivered")
                .readBy(new HashSet<>())
                .build();

        msgTable.save(message);

        conversation.setLastMessage(content);
        conversation.setTimestamp(LocalDateTime.now());

        if (!senderId.equals(receiverId)) {
            conversation.setUnread(conversation.getUnread() + 1);
        }

        convTable.save(conversation);

        messagingTemplate.convertAndSend(
                "/topic/messages/" + conversationId,
                convertToDto(message, sender)
        );

        return convertToDto(message, sender);
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
                .filter(msg -> !msg.getSenderId().equals(userId) && !msg.isReadBy(userId))
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
                .filter(msg -> !msg.getSenderId().equals(userId) && !msg.isReadBy(userId))
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

