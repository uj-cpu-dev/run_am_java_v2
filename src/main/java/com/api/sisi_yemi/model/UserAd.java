package com.api.sisi_yemi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UserAd {

    private String id;
    private String userId;
    private AdStatus status;
    private Instant datePosted;
    private String dedupeKey;

    private String title;
    private double price;
    private List<ImageData> images;
    private int views;
    private int messages;
    private Instant dateSold;
    private String category;
    private String location;
    private String condition;
    private String description;

    // === PRIMARY KEY ===
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    // === GSI: userId-index ===
    @DynamoDbSecondaryPartitionKey(indexNames = "userId-index")
    public String getUserId() {
        return userId;
    }

    // === GSI: userId-status-index ===
    @JsonIgnore
    @DynamoDbSecondaryPartitionKey(indexNames = "userId-status-index")
    public String getUserIdForStatusIndex() {
        return userId;
    }

    @JsonIgnore
    @DynamoDbSecondarySortKey(indexNames = "userId-status-index")
    @DynamoDbAttribute("status") // Required so DynamoDB Enhanced maps to correct attribute
    public String getStatusForUserIndex() {
        return status != null ? status.name() : null;
    }

    // === GSI: status-datePosted-index ===
    @JsonIgnore
    @DynamoDbSecondaryPartitionKey(indexNames = "status-datePosted-index")
    @DynamoDbAttribute("status") // ✅ Mapped to the real partition key in GSI
    public String getStatusForDateIndex() {
        return status != null ? status.name() : null;
    }

    @JsonIgnore
    @DynamoDbSecondarySortKey(indexNames = "status-datePosted-index")
    public Instant getDatePosted() {
        return datePosted;
    }

    // === GSI: dedupe-index ===
    @JsonIgnore
    @DynamoDbSecondaryPartitionKey(indexNames = "dedupe-index")
    public String getDedupeKey() {
        return dedupeKey;
    }

    public void computeDedupeKey() {
        if (userId != null && title != null && category != null && status != null) {
            this.dedupeKey = userId + "#" + title + "#" + price + "#" + category + "#" + status.name();
        }
    }

    // === Nested Class ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @DynamoDbBean
    public static class ImageData {
        private String id;
        private String url;
        private String s3key;
    }

    public enum AdStatus {
        ACTIVE, SOLD, DRAFT
    }
}
