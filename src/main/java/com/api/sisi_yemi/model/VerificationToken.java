package com.api.sisi_yemi.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@DynamoDbBean
public class VerificationToken {

    private String id;
    private String token;
    private String userId;
    private Instant expiryDate;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
