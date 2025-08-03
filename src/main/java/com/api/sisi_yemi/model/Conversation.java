package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

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
    private String lastMessageId;

    private int participantUnread;  // Unread count for participant
    private int sellerUnread;      // Unread count for seller

    // Helper method to get unread count for a user
    public int getUnreadForUser(String userId) {
        if (userId.equals(participantId)) {
            return participantUnread;
        } else if (userId.equals(sellerId)) {
            return sellerUnread;
        }
        return 0;
    }

    @DynamoDbAttribute("lastMessageId")
    public String getLastMessageId() {
        return lastMessageId;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    // GSI for querying by participant or seller
    @DynamoDbSecondaryPartitionKey(indexNames = "sellerId-index")
    public String getSellerId() {
        return sellerId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"participantId-index", "participantId-userAdId-index"})
    public String getParticipantId() {
        return participantId;
    }

    @DynamoDbSecondarySortKey(indexNames = "participantId-userAdId-index")
    public String getUserAdId() {
        return userAdId;
    }
}

