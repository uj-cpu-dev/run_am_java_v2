package com.api.sisi_yemi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger(DynamoStartupLogger.class);

    @Bean
    public CommandLineRunner testDynamoConnection(DynamoDbClient dynamoDbClient) {
        return args -> {
            try {
                dynamoDbClient.listTables();
                logger.info("✅ Successfully connected to DynamoDB.");
            } catch (Exception e) {
                logger.error("❌ Failed to connect to DynamoDB", e);
            }
        };
    }
}
