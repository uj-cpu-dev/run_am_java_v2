package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@DynamoDbBean
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
    private String provider;
    private String password;
    private Instant joinDate;
    private boolean enabled = false;
    private double rating;
    private int itemsSold;
    private int activeListings;
    private double responseRate;
    private String phone;
    private String location;
    private String bio;

    // Verification status
    private boolean emailVerified;
    private boolean phoneVerified;

    // Social media links (optional)
    private Map<String, String> socialLinks;

    // Preferences
    private List<String> favoriteCategories;

    private double reviews;
    private boolean isQuickResponder;
    private boolean isTopSeller;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() {
        return email;
    }
}
