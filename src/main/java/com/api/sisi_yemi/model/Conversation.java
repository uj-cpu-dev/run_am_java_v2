package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Conversation {
    private String id;
    private String participantId;
    private String sellerId;
    private String userAdId;
    private String lastMessage;
    private LocalDateTime timestamp;
    private int unread;
    private String status;

    private User participant;
    private User seller;
    private UserAd userAd;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    // GSI for querying by participant or seller
    @DynamoDbSecondaryPartitionKey(indexNames = {"participantId-index", "sellerId-index"})
    public String getParticipantId() {
        return participantId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "sellerId-index")
    public String getSellerId() {
        return sellerId;
    }

    @DynamoDbSecondarySortKey(indexNames = "participantId-userAdId-index")
    public String getUserAdId() {
        return userAdId;
    }
}

