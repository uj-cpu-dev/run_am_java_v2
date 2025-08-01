package com.api.sisi_yemi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class UserSettings {

    private String userId;

    // Notification settings
    private boolean emailNotifications;
    private boolean pushNotifications;
    private boolean smsNotifications;
    private boolean marketingNotifications;
    private boolean newMessageNotifications;
    private boolean itemUpdateNotifications;
    private boolean priceDropNotifications;

    // Privacy settings
    private boolean showPhoneNumber;
    private boolean showEmail;
    private boolean showOnlineStatus;
    private boolean publicProfile;

    private Instant lastUpdated;

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }
}
