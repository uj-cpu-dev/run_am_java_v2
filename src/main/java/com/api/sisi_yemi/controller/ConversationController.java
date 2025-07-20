package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.ConversationDto;
import com.api.sisi_yemi.dto.CreateConversationRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.repository.ConversationDynamoDbRepositoryImpl;
import com.api.sisi_yemi.service.ConversationService;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

    private static final Logger logger = LoggerFactory.getLogger(ConversationController.class);

    private final ConversationService conversationService;

    private final AuthenticationHelper authHelper;

    private final ConversationDynamoDbRepositoryImpl conversationRepository;

    private final DynamoDbUtilHelper dynamoDbUtilHelper;

    @GetMapping
    public ResponseEntity<?> getUserConversations() {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            List<ConversationDto> conversations = conversationService.getUserConversations(userId);
            return ResponseEntity.ok(conversations);
        } catch (ApiException e) {
            logger.warn("API Exception: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody CreateConversationRequest request) {
        try {
            String userId = authHelper.getAuthenticatedUserId();

            // Check if conversation already exists
            Optional<Conversation> existing = conversationRepository.findByParticipantAndUserAd(userId, request.getItemId());

            if (existing.isPresent()) {
                log.info("Existing conversation found for userId={} and itemId={}", userId, request.getItemId());
                ConversationDto existingDto = conversationService.convertToSecureDto(existing.get());
                return ResponseEntity.ok(existingDto); // 200 OK for existing
            }

            // Else, create new conversation
            ConversationDto conversation = conversationService.createConversation(userId, request.getItemId());
            return ResponseEntity.status(HttpStatus.CREATED).body(conversation); // 201 Created

        } catch (ApiException e) {
            logger.warn("API Exception while creating conversation: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error creating conversation", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable String conversationId) {
        String userId = authHelper.getAuthenticatedUserId();
        DynamoDbHelper<Conversation> convTable = dynamoDbUtilHelper.getConversationTable();
        Optional<Conversation> optionalConversation = convTable.getById(conversationId);

        if (optionalConversation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Conversation not found with ID: " + conversationId);
        }

        Conversation conversation = optionalConversation.get();

        // Check if the user is allowed to delete the conversation
        boolean isAuthorized = conversation.getParticipant().getId().equals(userId) ||
                conversation.getSeller().getId().equals(userId);

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You are not authorized to delete this conversation.");
        }

        convTable.deleteById(conversationId);
        return ResponseEntity.ok().body("Conversation deleted successfully.");
    }
}