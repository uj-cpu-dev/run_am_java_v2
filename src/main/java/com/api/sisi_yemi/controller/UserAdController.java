package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.AdDetailsResponse;
import com.api.sisi_yemi.dto.RecentActiveAdResponse;
import com.api.sisi_yemi.dto.UserAdResponse;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.service.AdDetailsService;
import com.api.sisi_yemi.service.UserAdService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdController {

    private static final Logger logger = LoggerFactory.getLogger(UserAdController.class);

    private final UserAdService userAdService;

    private final AuthenticationHelper authHelper;

    private final AdDetailsService adDetailsService;

    @GetMapping("/user/userAds")
    public ResponseEntity<?> getAllUserAds() {
        String userId = authHelper.getAuthenticatedUserId();
        List<UserAd> ads = userAdService.getAllAdsByUserId(userId);

        List<UserAdResponse> responseAds = ads.stream()
                .map(UserAdResponse::fromEntity)
                .toList();

        if (ads.isEmpty()) {
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                    "message", "No ads found for this user.",
                    "ads", responseAds
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "User ads retrieved successfully.",
                "ads", responseAds
        ));
    }

    @PostMapping("/post-ad")
    public ResponseEntity<?> createAd(@RequestBody UserAd userAd) {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            userAdService.createAdWithImages(userAd, userId);
            return ResponseEntity.ok().body("Ad has been posted successfully!");
        } catch (ApiException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .body(Map.of(
                            "error", ex.getMessage(),
                            "code", ex.getErrorCode()
                    ));
        } catch (Exception e) {
            logger.error("Error creating ad", e);
            return ResponseEntity
                    .internalServerError()
                    .body(Map.of("error", "Error processing your request"));
        }
    }

    @PutMapping("/update-ad")
    public ResponseEntity<?> updateAd(
            @RequestBody UserAd userAd) {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            userAdService.updateAdWithImages(userAd, userId);
            return ResponseEntity.ok().body("Ad has been updated successfully!");
        } catch (ApiException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .body(Map.of(
                            "error", ex.getMessage(),
                            "code", ex.getErrorCode()
                    ));
        } catch (Exception e) {
            logger.error("Error updating ad", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating ad");
        }
    }

    @DeleteMapping("/delete/{adId}/userAd")
    public ResponseEntity<?> deleteSingleAd(
            @PathVariable String adId) {

        try {
            String userId = authHelper.getAuthenticatedUserId();
            userAdService.deleteSingleAd(adId, userId);
            return ResponseEntity.ok(Map.of(
                    "message",
                    String.format("Ad with ID %s deleted successfully for user %s.", adId, userId)
            ));
        } catch (Exception e) {
            logger.error("Error deleting ad", e);
            return ResponseEntity.internalServerError()
                    .body("Error processing your request");
        }
    }

    @DeleteMapping("/delete/userAds")
    public ResponseEntity<?> deleteAllUserAds() {
        try {
            String userId = authHelper.getAuthenticatedUserId();
            userAdService.deleteAllAdsByUserId(userId);
            return ResponseEntity.ok(Map.of(
                    "message",
                    "All ads deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deleting all ad", e);
            return ResponseEntity.internalServerError()
                    .body("Error processing your request");
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentActiveAdResponse>> getRecentActiveAds() {
        List<RecentActiveAdResponse> recentAds = userAdService.getRecentActiveAds();
        return ResponseEntity.ok(recentAds);
    }

    @GetMapping("/{adId}/userAd")
    public ResponseEntity<AdDetailsResponse> getUserAdById(@PathVariable String adId) {
        return ResponseEntity.ok(adDetailsService.getAdDetails(adId));
    }
}
