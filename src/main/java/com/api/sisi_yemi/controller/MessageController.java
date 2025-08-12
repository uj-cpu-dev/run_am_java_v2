package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.SendMessageRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.service.MessageService;
import com.api.sisi_yemi.service.UploadService;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;
    private final AuthenticationHelper authHelper;
    private final UploadService uploadService;

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversationMessages(@PathVariable String conversationId) {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            List<MessageDto> messages = messageService.getMessages(conversationId, userId);
            return ResponseEntity.ok(messages);
        } catch (ApiException e) {
            logger.warn("API Exception while fetching messages: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error fetching messages", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @PostMapping("/{conversationId}")
    public ResponseEntity<?> sendMessage(@PathVariable String conversationId,
                                         @RequestBody SendMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty() && request.getAttachmentUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content cannot be empty");
        }

        try {
            String userId = authHelper.getAuthenticatedUserId();
            MessageDto message = messageService.sendMessageHttp(conversationId, userId, request.getContent(), request.getAttachmentUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (ApiException e) {
            logger.warn("API Exception while sending message: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending message", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable String conversationId) {
        String userId = authHelper.getAuthenticatedUserId();
        messageService.markMessagesAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{conversationId}/{messageId}")
    public ResponseEntity<?> editMessage(@PathVariable String conversationId,
                                         @PathVariable String messageId,
                                         @RequestBody SendMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message content cannot be empty");
        }

        try {
            String userId = authHelper.getAuthenticatedUserId();
            MessageDto updatedMessage = messageService.editMessage(conversationId, messageId, request.getContent(), userId);
            return ResponseEntity.ok(updatedMessage);
        } catch (ApiException e) {
            logger.warn("API Exception while editing message: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error editing message", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @DeleteMapping("/{conversationId}/{messageId}")
    public ResponseEntity<?> deleteMessage(@PathVariable String conversationId,
                                           @PathVariable String messageId) {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            messageService.deleteMessage(conversationId, messageId, userId);
            return ResponseEntity.noContent().build();
        } catch (ApiException e) {
            logger.warn("API Exception while deleting message: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error deleting message", e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }

    @PostMapping("/{conversationId}/attachment-url")
    public ResponseEntity<?> generateAttachmentPresignedUrl(
            @PathVariable String conversationId,
            @RequestBody UploadController.PresignedUrlRequest request) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            UploadController.PresignedUrlResponse presignedUrl = uploadService.generatePresignedUrl(
                    conversationId,
                    request.filename(),
                    request.contentType(),
                    "message-attachments"
            );

            return ResponseEntity.ok(presignedUrl);
        } catch (ApiException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating presigned URL");
        }
    }
}
