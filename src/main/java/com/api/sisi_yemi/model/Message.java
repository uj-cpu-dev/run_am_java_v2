package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@DynamoDbBean
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    private String conversationId;
    private String messageId;
    private String senderId;
    private String content;
    private String status;
    private LocalDateTime timestamp;

    private Set<String> readBy = new HashSet<>();

    // Add helper methods
    public boolean isReadBy(String userId) {
        return readBy.contains(userId);
    }

    public void markReadBy(String userId) {
        readBy.add(userId);
    }

    @DynamoDbPartitionKey
    public String getConversationId() {
        return conversationId;
    }

    @DynamoDbSortKey
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

