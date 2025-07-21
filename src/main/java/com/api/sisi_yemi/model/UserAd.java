package com.api.sisi_yemi.model;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

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

    // Partition Key
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    // GSI: userId-index
    @DynamoDbSecondaryPartitionKey(indexNames = "userId-index")
    public String getUserId() {
        return userId;
    }

    // GSI: userId-status-index
    @DynamoDbSecondaryPartitionKey(indexNames = "userId-status-index")
    public String getUserIdForStatusIndex() {
        return userId != null ? userId : null;
    }

    @DynamoDbSecondarySortKey(indexNames = "userId-status-index")
    public AdStatus getStatus() {
        return status;
    }

    // GSI: status-datePosted-index
    @DynamoDbSecondaryPartitionKey(indexNames = "status-datePosted-index")
    public AdStatus getStatusForDateIndex() {
        return status;
    }

    @DynamoDbSecondarySortKey(indexNames = "status-datePosted-index")
    public Instant getDatePosted() {
        return datePosted;
    }

    // GSI: dedupe-index
    @DynamoDbSecondaryPartitionKey(indexNames = "dedupe-index")
    public String getDedupeKey() {
        return dedupeKey;
    }

    public void computeDedupeKey() {
        if (userId != null && title != null && category != null && status != null) {
            this.dedupeKey = userId + "#" + title + "#" + price + "#" + category + "#" + status.name();
        }
    }

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
