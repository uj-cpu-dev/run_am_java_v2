package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.UserSettingsResponse;
import com.api.sisi_yemi.dto.UserSettingsUpdateRequest;
import com.api.sisi_yemi.model.UserSettings;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    private final DynamoDbHelperFactory dynamoDbHelperFactory;

    private final String USER_SETTINGS_TABLE;

    public UserSettingsService(DynamoDbHelperFactory dynamoDbHelperFactory, @Value("${users.settings.table}")String USER_SETTINGS_TABLE) {
        this.dynamoDbHelperFactory = dynamoDbHelperFactory;
        this.USER_SETTINGS_TABLE = USER_SETTINGS_TABLE;
    }

    public UserSettings getSettings(String userId) {
        DynamoDbHelper<UserSettings> userHelper = dynamoDbHelperFactory.getHelper(USER_SETTINGS_TABLE, UserSettings.class);
        return userHelper.getById(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    public void updateSettings(String userId, UserSettingsUpdateRequest updates) {
        UserSettings settings = getExistingOrCreateDefault(userId);

        // Notification settings
        if (updates.getEmailNotifications() != null)
            settings.setEmailNotifications(updates.getEmailNotifications());
        if (updates.getPushNotifications() != null)
            settings.setPushNotifications(updates.getPushNotifications());
        if (updates.getSmsNotifications() != null)
            settings.setSmsNotifications(updates.getSmsNotifications());
        if (updates.getMarketingNotifications() != null)
            settings.setMarketingNotifications(updates.getMarketingNotifications());
        if (updates.getNewMessageNotifications() != null)
            settings.setNewMessageNotifications(updates.getNewMessageNotifications());
        if (updates.getItemUpdateNotifications() != null)
            settings.setItemUpdateNotifications(updates.getItemUpdateNotifications());
        if (updates.getPriceDropNotifications() != null)
            settings.setPriceDropNotifications(updates.getPriceDropNotifications());

        // Privacy settings
        if (updates.getShowPhoneNumber() != null)
            settings.setShowPhoneNumber(updates.getShowPhoneNumber());
        if (updates.getShowEmail() != null)
            settings.setShowEmail(updates.getShowEmail());
        if (updates.getShowOnlineStatus() != null)
            settings.setShowOnlineStatus(updates.getShowOnlineStatus());
        if (updates.getPublicProfile() != null)
            settings.setPublicProfile(updates.getPublicProfile());

        DynamoDbHelper<UserSettings> userHelper = dynamoDbHelperFactory.getHelper(USER_SETTINGS_TABLE, UserSettings.class);

        userHelper.save(settings);
    };


    private UserSettings getExistingOrCreateDefault(String userId) {
        DynamoDbHelper<UserSettings> userHelper = dynamoDbHelperFactory.getHelper(USER_SETTINGS_TABLE, UserSettings.class);
        return userHelper.getById(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    private UserSettings createDefaultSettings(String userId) {
        return UserSettings.builder()
                .userId(userId)
                .emailNotifications(true)
                .pushNotifications(true)
                .newMessageNotifications(true)
                .itemUpdateNotifications(true)
                .showOnlineStatus(true)
                .publicProfile(true)
                .build();
    }

    public UserSettingsResponse toResponse(UserSettings settings) {
        return UserSettingsResponse.builder()
                .emailNotifications(settings.isEmailNotifications())
                .pushNotifications(settings.isPushNotifications())
                .smsNotifications(settings.isSmsNotifications())
                .marketingNotifications(settings.isMarketingNotifications())
                .newMessageNotifications(settings.isNewMessageNotifications())
                .itemUpdateNotifications(settings.isItemUpdateNotifications())
                .priceDropNotifications(settings.isPriceDropNotifications())
                .showPhoneNumber(settings.isShowPhoneNumber())
                .showEmail(settings.isShowEmail())
                .showOnlineStatus(settings.isShowOnlineStatus())
                .publicProfile(settings.isPublicProfile())
                .build();
    }
}
