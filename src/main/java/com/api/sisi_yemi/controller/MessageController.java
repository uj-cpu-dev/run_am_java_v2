package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.MessageDto;
import com.api.sisi_yemi.dto.SendMessageRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.service.MessageService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/messages")
@RequiredArgsConstructor
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;

    private final AuthenticationHelper authHelper;

    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable String conversationId) {

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
    public ResponseEntity<?> sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {

        if(request.getContent().isEmpty()){
            return null;
        }

        try {
            String userId = authHelper.getAuthenticatedUserId();
            MessageDto message = messageService.sendMessageHttp(conversationId, userId, request.getContent());
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
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable("conversationId") String conversationId) {
        String userId = authHelper.getAuthenticatedUserId();
        messageService.markMessagesAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }
}
