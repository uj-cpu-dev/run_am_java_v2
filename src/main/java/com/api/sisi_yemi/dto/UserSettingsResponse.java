package com.api.sisi_yemi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSettingsResponse {

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
}
