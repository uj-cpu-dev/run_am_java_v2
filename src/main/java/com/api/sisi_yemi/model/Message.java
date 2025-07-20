package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;

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
    private boolean read;
    private LocalDateTime timestamp;

    @DynamoDbPartitionKey
    public String getConversationId() {
        return conversationId;
    }

    @DynamoDbSortKey
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}

