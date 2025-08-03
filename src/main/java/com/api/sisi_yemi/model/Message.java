package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class Message {
    private String conversationId;
    private String messageId;
    private String senderId;
    private String content;
    private String status;
    private LocalDateTime timestamp;
    private Set<String> readBy;

    @DynamoDbPartitionKey
    public String getConversationId() {
        return conversationId;
    }

    @DynamoDbSortKey
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @DynamoDbAttribute("readBy")
    public Set<String> getReadBy() {
        if (readBy == null || readBy.isEmpty()) {
            return null; // Return null instead of empty set
        }
        return readBy;
    }

    public void setReadBy(Set<String> readBy) {
        this.readBy = (readBy == null || readBy.isEmpty()) ? null : readBy;
    }

    // Helper method to check if read by user
    public boolean isReadBy(String userId) {
        return readBy != null && readBy.contains(userId);
    }

    // Helper method to mark as read
    public void markReadBy(String userId) {
        if (readBy == null) {
            readBy = new HashSet<>();
        }
        readBy.add(userId);
    }
}

