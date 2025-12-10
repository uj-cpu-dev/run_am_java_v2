package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.FilteredAdResponse;
import com.api.sisi_yemi.dto.RecentActiveAdResponse;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepositoryImpl;
import com.api.sisi_yemi.util.AdValidator;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.FilterAdHelper;
import com.api.sisi_yemi.util.ImageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.api.sisi_yemi.util.FilterAdHelper.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdService {

    private final UserAdDynamoDbRepositoryImpl userAdRepository;
    private final AdValidator adValidator;
    private final ImageUploader imageUploader;
    private final DynamoDbUtilHelper dynamoDbUtilHelper;
    private final FilterAdHelper filterAdHelper;

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

        userAd.setId(userAd.getId());
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
        try {
            if (ad == null) return null;

            return RecentActiveAdResponse.builder()
                    .id(ad.getId() != null ? ad.getId() : "unknown")
                    .title(ad.getTitle() != null ? ad.getTitle() : "No Title")
                    .price(ad.getPrice())
                    .category(ad.getCategory() != null ? ad.getCategory() : "Uncategorized")
                    .description(ad.getDescription() != null ? ad.getDescription() : "")
                    .location(ad.getLocation() != null ? ad.getLocation() : "Unknown Location")
                    .condition(ad.getCondition() != null ? ad.getCondition() : "Unknown")
                    .images(ad.getImages() != null ? ad.getImages() : Collections.emptyList())
                    .views(ad.getViews())
                    .messages(ad.getMessages())
                    .datePosted(ad.getDatePosted() != null ? ad.getDatePosted() : Instant.now())
                    .status(UserAd.AdStatus.valueOf(ad.getStatus() != null ? ad.getStatus().name() : "UNKNOWN"))
                    .dateSold(ad.getDateSold())
                    .build();
        } catch (Exception e) {
            log.error("Error mapping ad to response: {}", ad.getId(), e);
            return null;
        }
    }

    /*public FilteredAdResponse filterAds(
            String statusStr,
            String category,
            String location,
            String condition,
            Double minPrice,
            Double maxPrice,
            String search,
            String sortBy,
            String sortDir,
            Map<String, String> paginationToken
    ) {
        UserAd.AdStatus status = FilterAdHelper.parseStatus(statusStr);

        DynamoDbIndex<UserAd> index = dynamoDbUtilHelper.getUserAdsTable().getRawTable()
                .index("status-datePosted-index");

        QueryEnhancedRequest queryRequest = buildQueryRequest(status, sortDir, paginationToken);

        SdkIterable<Page<UserAd>> sdkPages = index.query(queryRequest);
        Iterator<Page<UserAd>> iterator = sdkPages.iterator();

        List<UserAd> filteredAds = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        if (iterator.hasNext()) {
            Page<UserAd> page = iterator.next();
            filteredAds.addAll(filterPageItems(page.items(), category, location, condition, minPrice, maxPrice, search));
            lastEvaluatedKey = page.lastEvaluatedKey();
        }

        sortResults(filteredAds, sortBy, sortDir);

        List<RecentActiveAdResponse> responseItems = convertToResponse(filteredAds);

        Map<String, String> nextToken = buildPaginationToken(lastEvaluatedKey);
        boolean hasNextPage = nextToken != null && !nextToken.isEmpty();

        return new FilteredAdResponse(responseItems, nextToken, hasNextPage);
    }

    private List<RecentActiveAdResponse> convertToResponse(List<UserAd> userAds) {
        return userAds.stream()
                .map(this::mapToRecentActiveAdResponse)
                .collect(Collectors.toList());
    }*/

    public FilteredAdResponse filterAds(
            String statusStr,
            String category,
            String location,
            String condition,
            Double minPrice,
            Double maxPrice,
            String search,
            String sortBy,
            String sortDir,
            Map<String, String> paginationToken
    ) {
        validateFilterParameters(category, location, condition, minPrice, maxPrice, search);
        UserAd.AdStatus status = FilterAdHelper.parseStatus(statusStr);

        DynamoDbIndex<UserAd> index = dynamoDbUtilHelper.getUserAdsTable().getRawTable()
                .index("status-datePosted-index");

        QueryEnhancedRequest queryRequest = buildQueryRequest(status, sortDir, paginationToken);

        SdkIterable<Page<UserAd>> sdkPages = index.query(queryRequest);
        Iterator<Page<UserAd>> iterator = sdkPages.iterator();

        List<UserAd> filteredAds = new ArrayList<>();
        Map<String, AttributeValue> lastEvaluatedKey = null;

        if (iterator.hasNext()) {
            Page<UserAd> page = iterator.next();
            filteredAds.addAll(filterAdHelper.filterPageItems(page.items(), category, location, condition, minPrice, maxPrice, search));
            lastEvaluatedKey = page.lastEvaluatedKey();
        }

        filterAdHelper.sortResults(filteredAds, sortBy, sortDir);

        List<RecentActiveAdResponse> responseItems = convertToResponse(filteredAds);

        Map<String, String> nextToken = buildPaginationToken(lastEvaluatedKey);
        boolean hasNextPage = nextToken != null && !nextToken.isEmpty();

        return new FilteredAdResponse(responseItems, nextToken, hasNextPage);
    }

    private List<RecentActiveAdResponse> convertToResponse(List<UserAd> userAds) {
        return userAds.stream()
                .map(this::mapToRecentActiveAdResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void validateFilterParameters(String category, String location,
                                          String condition, Double minPrice,
                                          Double maxPrice, String search) {

        if (search != null && search.length() > 100) {
            throw new IllegalArgumentException("Search term too long");
        }

        if (category != null && category.length() > 50) {
            throw new IllegalArgumentException("Category filter too long");
        }

        if (location != null && location.length() > 100) {
            throw new IllegalArgumentException("Location filter too long");
        }

        if (condition != null) {
            List<String> validConditions = Arrays.asList("new", "used", "refurbished");
            if (!validConditions.contains(condition.toLowerCase())) {
                throw new IllegalArgumentException("Invalid condition value");
            }
        }

        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("Minimum price cannot be negative");
        }

        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("Maximum price cannot be negative");
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
    }
}
