package com.api.sisi_yemi.util;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeEndpointsRequest;

@Component
public class DynamoDbHealthIndicator implements HealthIndicator {

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbHealthIndicator(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    @Override
    public Health health() {
        try {
            // Lightweight operation — no need to touch any table
            dynamoDbClient.describeEndpoints(DescribeEndpointsRequest.builder().build());

            return Health.up()
                    .withDetail("DynamoDB", "Available")
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("DynamoDB", "Unavailable")
                    .build();
        }
    }
}
