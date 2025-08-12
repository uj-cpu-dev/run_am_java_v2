package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.UserDto;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.model.Message;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.repository.ConversationDynamoDbRepositoryImpl;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ConversationDynamoDbRepositoryImpl conversationRepository;

    public MessageDto sendMessageHttp(String conversationId, String senderId, String content, String attachmentUrl) {
        if((content == null || content.trim().isEmpty()) && (attachmentUrl == null || attachmentUrl.trim().isEmpty())){
            throw new ApiException("Either message content or attachment must be provided", BAD_REQUEST, "EMPTY_MESSAGE");
        }

        var msgTable = dynamoDbUtilHelper.getMessageTable();
        var convTable = dynamoDbUtilHelper.getConversationTable();
        var userTable = dynamoDbUtilHelper.getUserTable();

        Conversation conversation = getConversationWithValidation(convTable, conversationId, senderId);
        User sender = getUserWithValidation(userTable, senderId);

        Message message = createMessage(conversationId, senderId, content, attachmentUrl);
        msgTable.save(message);

        updateConversationAfterMessage(convTable, conversation, message, senderId);

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
        var convTable = dynamoDbUtilHelper.getConversationTable();

        Message message = msgTable.getByPartitionKey(conversationId).stream()
                .filter(m -> m.getMessageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Message not found", NOT_FOUND, "MESSAGE_NOT_FOUND"));

        if (!message.getSenderId().equals(userId)) {
            throw new ApiException("You can only edit your own messages", FORBIDDEN, "EDIT_NOT_ALLOWED");
        }

        // Only update if content changed
        if (!message.getContent().equals(content)) {
            message.setContent(content);
            message.setEdited(true);
            message.setEditedAt(LocalDateTime.now());
            msgTable.save(message);

            Conversation conversation = getConversationWithValidation(convTable, conversationId, userId);

            // If this was the last message in the conversation, update it
            if (messageId.equals(conversation.getLastMessageId())) {
                conversation.setLastMessage(content);
                conversation.setTimestamp(LocalDateTime.now());
                convTable.save(conversation);
            }
        }

        User sender = getUserWithValidation(userTable, userId);
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

        // If deleted message was last message, update conversation
        if (messageId.equals(conversation.getLastMessageId())) {
            updateConversationLastMessage(conversation, msgTable);
        }
    }

    // ========== Private Helpers ==========

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

    private Message createMessage(String conversationId, String senderId, String content, String attachmentUrl) {
        Message.MessageBuilder builder = Message.builder()
                .conversationId(conversationId)
                .messageId(UUID.randomUUID().toString())
                .senderId(senderId)
                .timestamp(LocalDateTime.now())
                .status("delivered");

        if (content != null && !content.trim().isEmpty()) {
            builder.content(content);
        }

        if (attachmentUrl != null && !attachmentUrl.trim().isEmpty()) {
            builder.attachmentUrl(attachmentUrl);
            builder.attachmentType(determineAttachmentType(attachmentUrl));
        }

        return builder.build();
    }

    private String determineAttachmentType(String url) {
        if (url == null) return null;

        String extension = url.substring(url.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return "image";
            case "mp4":
            case "mov":
            case "avi":
                return "video";
            case "mp3":
            case "wav":
                return "audio";
            case "pdf":
                return "pdf";
            default:
                return "file";
        }
    }

    private void updateConversationAfterMessage(DynamoDbHelper<Conversation> convTable,
                                                Conversation conversation,
                                                Message message,
                                                String senderId) {
        String receiverId = senderId.equals(conversation.getParticipantId())
                ? conversation.getSellerId()
                : conversation.getParticipantId();

        // Set last message content - use attachment indicator if no text content
        String lastMessageContent = message.getContent();
        if ((lastMessageContent == null || lastMessageContent.isEmpty()) && message.getAttachmentUrl() != null) {
            lastMessageContent = "[Attachment]";
        }

        conversation.setLastMessage(lastMessageContent);
        conversation.setLastMessageId(message.getMessageId());
        conversation.setTimestamp(message.getTimestamp());

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
            String lastMessageContent = last.getContent();
            if ((lastMessageContent == null || lastMessageContent.isEmpty()) && last.getAttachmentUrl() != null) {
                lastMessageContent = "[Attachment]";
            }

            conversation.setLastMessage(lastMessageContent);
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

    public List<String> getParticipantUserIds(String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        return conversation.getParticipantUserIds();
    }

    public int getUnreadCount(String conversationId, String userId) {
        var convTable = dynamoDbUtilHelper.getConversationTable();
        Conversation conversation = convTable.getById(conversationId)
                .orElseThrow(() -> new ApiException("Conversation not found", NOT_FOUND, "CONVERSATION_NOT_FOUND"));

        if (userId.equals(conversation.getParticipantId())) {
            return conversation.getParticipantUnread();
        } else if (userId.equals(conversation.getSellerId())) {
            return conversation.getSellerUnread();
        }
        throw new ApiException("Access denied", FORBIDDEN, "ACCESS_DENIED");
    }

    private MessageDto convertToDto(Message message, User sender) {
        var dto = new MessageDto();
        dto.setId(message.getMessageId());
        dto.setConversationId(message.getConversationId());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setTimestamp(message.getTimestamp());
        dto.setSender(convertUserDto(sender));
        dto.setEdited(message.isEdited());
        dto.setEditedAt(message.getEditedAt());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setAttachmentType(message.getAttachmentType());
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