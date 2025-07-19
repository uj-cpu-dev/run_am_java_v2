package com.api.sisi_yemi.model;

import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@DynamoDbBean
@Builder
public class User {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
    private String provider;
    private String password;
    private Instant joinDate;
    private boolean enabled = false;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
