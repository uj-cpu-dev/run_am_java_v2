package com.api.sisi_yemi.util.dynamodb;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Component
public class DynamoDbHelperFactory {

    private final DynamoDbClient dynamoDbClient;

    public DynamoDbHelperFactory(DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }

    public <T> DynamoDbHelper<T> getHelper(String tableName, Class<T> clazz) {
        return new DynamoDbHelper<>(dynamoDbClient, tableName, clazz);
    }

}
