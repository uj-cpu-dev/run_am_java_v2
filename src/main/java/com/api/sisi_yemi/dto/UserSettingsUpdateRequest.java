package com.api.sisi_yemi.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class UserSettingsUpdateRequest {

    // Notification settings
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean marketingNotifications;
    private Boolean newMessageNotifications;
    private Boolean itemUpdateNotifications;
    private Boolean priceDropNotifications;

    // Privacy settings
    private Boolean showPhoneNumber;
    private Boolean showEmail;
    private Boolean showOnlineStatus;
    private Boolean publicProfile;
}
