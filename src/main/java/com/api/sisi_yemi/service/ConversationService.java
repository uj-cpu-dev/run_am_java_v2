package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.ConversationDto;
import com.api.sisi_yemi.dto.ItemDto;
import com.api.sisi_yemi.dto.UserDto;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.ConversationDynamoDbRepository;
import com.api.sisi_yemi.repository.ConversationDynamoDbRepositoryImpl;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepository;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationDynamoDbRepositoryImpl conversationRepository;
    private final DynamoDbUtilHelper dynamoDb;
    private final UserAdDynamoDbRepository userAdRepository;

    public List<ConversationDto> getUserConversations(String userId) {
        try {
            return conversationRepository.findByUser(userId)
                    .stream()
                    .map(conv -> convertToSecureDto(conv, userId)) // Pass userId
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ApiException("Failed to retrieve conversations", INTERNAL_SERVER_ERROR, "CONVERSATION_RETRIEVAL_ERROR");
        }
    }

    public ConversationDto createConversation(String userId, String itemId) {
        User buyer = dynamoDb.getUserTable().getById(userId)
                .orElseThrow(() -> new ApiException("User not found", NOT_FOUND));

        UserAd item = userAdRepository.findById(itemId)
                .orElseThrow(() -> new ApiException("Item not found", NOT_FOUND));

        User seller = dynamoDb.getUserTable().getById(item.getUserId())
                .orElseThrow(() -> new ApiException("Seller not found", NOT_FOUND));

        return createNewSecureConversation(buyer, seller, item);
    }

    private ConversationDto createNewSecureConversation(User buyer, User seller, UserAd item) {
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .participantId(buyer.getId())
                .sellerId(seller.getId())
                .userAdId(item.getId())
                .participant(buyer)
                .seller(seller)
                .userAd(item)
                .lastMessage("")
                .timestamp(LocalDateTime.now())
                .unread(0)
                .status("active")
                .build();

        Conversation saved = conversationRepository.save(conversation);
        return convertToSecureDto(saved, buyer.getId());
    }

    public ConversationDto convertToSecureDto(Conversation conversation, String currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());

        if (currentUserId.equals(conversation.getParticipantId())) {
            dto.setParticipant(convertToUserDto(conversation.getSeller()));
        } else {
            dto.setParticipant(convertToUserDto(conversation.getParticipant()));
        }

        dto.setItem(convertUserAdToItemDto(conversation.getUserAd()));
        dto.setLastMessage(conversation.getLastMessage());
        dto.setTimestamp(conversation.getTimestamp());
        dto.setUnread(conversation.getUnreadForUser(currentUserId)); // User-specific unread count
        dto.setStatus(conversation.getStatus());

        return dto;
    }

    private UserDto convertToUserDto(User user) {
        UserDto dto = new UserDto();
        // Never include user ID in DTO
        dto.setName(user.getName());
        dto.setAvatar(user.getAvatarUrl());
        dto.setVerified(user.isEmailVerified() && user.isPhoneVerified());
        dto.setRating(user.getRating());
        return dto;
    }

    private ItemDto convertUserAdToItemDto(UserAd userAd) {
        ItemDto dto = new ItemDto();
        dto.setId(userAd.getId());
        dto.setTitle(userAd.getTitle());
        dto.setPrice(userAd.getPrice());
        dto.setImage(userAd.getImages() != null && !userAd.getImages().isEmpty() ? userAd.getImages().get(0).getUrl() : null);
        return dto;
    }
}
