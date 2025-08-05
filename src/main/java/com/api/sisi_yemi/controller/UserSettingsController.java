package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.UserSettingsResponse;
import com.api.sisi_yemi.dto.UserSettingsUpdateRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserSettings;
import com.api.sisi_yemi.service.UserSettingsService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final AuthenticationHelper authHelper;

    private final UserSettingsService userSettingsService;

    @GetMapping
    public ResponseEntity<UserSettingsResponse> getSettings() {
        String userId = authHelper.getAuthenticatedUserId();
        UserSettings settings = userSettingsService.getSettings(userId);
        return ResponseEntity.ok(userSettingsService.toResponse(settings));
    }

    @PatchMapping
    public ResponseEntity<?> updateSettings(
            @RequestBody UserSettingsUpdateRequest updates) {
        String userId = authHelper.getAuthenticatedUserId();
        userSettingsService.updateSettings(userId, updates);
        return ResponseEntity.ok("User Settings Updated Successfully");
    }

    @PostMapping("/request-data")
    public ResponseEntity<?> requestDataDownload() {
        String userId = authHelper.getAuthenticatedUserId();

        if(userId.isEmpty()){
           throw new ApiException("NOT_AUTHORISED", HttpStatus.FORBIDDEN, "NOT_AUTHORISED");
        }

        return ResponseEntity.ok("Data download request received for user");
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount() {
        String userId = authHelper.getAuthenticatedUserId();

        if(userId.isEmpty()){
            throw new ApiException("NOT_AUTHORISED", HttpStatus.FORBIDDEN, "NOT_AUTHORISED");
        }

        return ResponseEntity.ok("Account deletion request received for user");
    }
}