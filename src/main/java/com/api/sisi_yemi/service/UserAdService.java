package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.RecentActiveAdResponse;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepository;
import com.api.sisi_yemi.util.AdValidator;
import com.api.sisi_yemi.util.ImageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdService {

    private final UserAdDynamoDbRepository userAdRepository;
    private final AdValidator adValidator;
    private final ImageUploader imageUploader;

    public List<UserAd> getUserAdsByStatus(String userId, UserAd.AdStatus status) {
        return userAdRepository.findByUserIdAndStatus(userId, status);
    }

    public Map<String, Object> getUserAdStats(String userId) {
        long activeCount = userAdRepository.countByUserIdAndStatus(userId, UserAd.AdStatus.ACTIVE);
        long soldCount = userAdRepository.countByUserIdAndStatus(userId, UserAd.AdStatus.SOLD);
        long draftCount = userAdRepository.countByUserIdAndStatus(userId, UserAd.AdStatus.DRAFT);
        long totalCount = userAdRepository.countByUserId(userId);

        List<UserAd> allAds = userAdRepository.findByUserId(userId);
        int totalViews = allAds.stream().mapToInt(UserAd::getViews).sum();
        int totalMessages = allAds.stream().mapToInt(UserAd::getMessages).sum();
        double totalEarnings = allAds.stream()
                .filter(ad -> ad.getStatus() == UserAd.AdStatus.SOLD)
                .mapToDouble(UserAd::getPrice)
                .sum();

        return Map.of(
                "activeCount", activeCount,
                "soldCount", soldCount,
                "draftCount", draftCount,
                "totalCount", totalCount,
                "totalViews", totalViews,
                "totalMessages", totalMessages,
                "totalEarnings", totalEarnings
        );
    }

    public void createAdWithImages(UserAd userAd, String userId) {
        userAd.setUserId(userId);
        adValidator.validateAdCreation(userAd);

        if (userAdRepository.existsDuplicate(userId, userAd.getTitle(), userAd.getPrice(),
                userAd.getCategory(), userAd.getStatus())) {
            throw new IllegalArgumentException("Duplicate ad detected");
        }

        userAd.setId(UUID.randomUUID().toString());
        userAd.setViews(0);
        userAd.setMessages(0);
        userAd.setDatePosted(Instant.now());
        userAd.setStatus(UserAd.AdStatus.ACTIVE);

        userAdRepository.save(userAd);
    }

    public void updateAdWithImages(UserAd updatedAd, String userId) {
        UserAd existingAd = userAdRepository.findById(updatedAd.getId())
                .orElseThrow(() -> new IllegalArgumentException("Ad not found"));

        if (!existingAd.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized update attempt");
        }

        if (userAdRepository.existsDuplicateExceptId(userId, updatedAd.getTitle(), updatedAd.getPrice(),
                updatedAd.getCategory(), updatedAd.getStatus(),
                updatedAd.getId())) {
            throw new IllegalArgumentException("Duplicate ad detected");
        }

        existingAd.setTitle(updatedAd.getTitle());
        existingAd.setDescription(updatedAd.getDescription());
        existingAd.setPrice(updatedAd.getPrice());
        existingAd.setCategory(updatedAd.getCategory());
        existingAd.setLocation(updatedAd.getLocation());
        existingAd.setImages(updatedAd.getImages());
        existingAd.setStatus(updatedAd.getStatus());
        existingAd.setDatePosted(Instant.now());

        adValidator.validateAdUpdate(existingAd);
        userAdRepository.save(existingAd);
    }

    public List<UserAd> getAllAdsByUserId(String userId) {
        return userAdRepository.findByUserId(userId);
    }

    public void deleteSingleAd(String adId, String userId) {
        UserAd ad = userAdRepository.findById(adId)
                .orElseThrow(() -> new ApiException("Ad not found", HttpStatus.NOT_FOUND, "AD_NOT_FOUND"));

        if (!ad.getUserId().equals(userId)) {
            throw new ApiException("Not authorized to delete this ad", HttpStatus.FORBIDDEN, "UNAUTHORIZED_OPERATION");
        }

        if (ad.getImages() != null) {
            ad.getImages().forEach(image -> {
                String key = imageUploader.extractS3KeyFromUrl(image.getUrl());
                imageUploader.deleteImageFromS3(key);
            });
        }

        userAdRepository.deleteById(adId);
    }

    public void deleteAllAdsByUserId(String userId) {
        List<UserAd> userAds = userAdRepository.findByUserId(userId);

        userAds.forEach(ad -> {
            if (ad.getImages() != null) {
                ad.getImages().forEach(image -> {
                    String key = imageUploader.extractS3KeyFromUrl(image.getUrl());
                    imageUploader.deleteImageFromS3(key);
                });
            }
            userAdRepository.deleteById(ad.getId());
        });
    }

    public List<RecentActiveAdResponse> getRecentActiveAds() {
        List<UserAd> activeAds = userAdRepository.findRecentActiveAds(10);

        return activeAds.stream()
                .map(this::mapToRecentActiveAdResponse)
                .collect(Collectors.toList());
    }

    public void deleteAllRecentActiveAds() {
        List<UserAd> activeAds = userAdRepository.findRecentActiveAds(10);
        activeAds.forEach(ad -> userAdRepository.deleteById(ad.getId()));
    }

    private RecentActiveAdResponse mapToRecentActiveAdResponse(UserAd ad) {
        return RecentActiveAdResponse.builder()
                .id(ad.getId())
                .title(ad.getTitle())
                .price(ad.getPrice())
                .category(ad.getCategory())
                .description(ad.getDescription())
                .location(ad.getLocation())
                .condition(ad.getCondition())
                .images(ad.getImages())
                .views(ad.getViews())
                .messages(ad.getMessages())
                .datePosted(ad.getDatePosted())
                .status(ad.getStatus())
                .dateSold(ad.getDateSold())
                .build();
    }
}
