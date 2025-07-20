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
    @Getter(onMethod_ = @DynamoDbPartitionKey)
    private String id;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "userId-index"))
    private String userId;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "userId-status-index"))
    private String userIdForStatusIndex;

    @Getter(onMethod_ = @DynamoDbSecondarySortKey(indexNames = "userId-status-index"))
    private AdStatus status;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "status-datePosted-index"))
    private AdStatus statusForDateIndex;

    @Getter(onMethod_ = @DynamoDbSecondarySortKey(indexNames = "status-datePosted-index"))
    private Instant datePosted;

    @Getter(onMethod_ = @DynamoDbSecondaryPartitionKey(indexNames = "dedupe-index"))
    private String dedupeKey;

    // Regular attributes
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

    public void computeDedupeKey() {
        if (userId != null && title != null && category != null && status != null) {
            this.dedupeKey = userId + "#" + title + "#" + price + "#" + category + "#" + status.name();
        }
    }
}