package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.UpdateProfileRequest;
import com.api.sisi_yemi.service.ProfileService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;

    private final AuthenticationHelper authHelper;

    @GetMapping
    public ResponseEntity<?> getProfile() {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            return ResponseEntity.ok(profileService.getProfile(userId));
        } catch (Exception e) {
            logger.error("Error getting user profile details", e);
            return ResponseEntity.internalServerError()
                    .body("Error processing your request");
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateProfileRequest request) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            profileService.updateProfile(userId, request);
            return ResponseEntity.ok().body("Profile Updated Successfully");
        } catch (Exception e) {
            logger.error("Error updating user profile details", e);
            return ResponseEntity.internalServerError()
                    .body("Error processing your request");
        }
    }
}