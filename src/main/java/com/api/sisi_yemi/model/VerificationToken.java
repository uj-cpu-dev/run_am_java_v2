package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
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
