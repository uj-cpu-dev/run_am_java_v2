package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.ProfilePreview;
import com.api.sisi_yemi.dto.ProfileResponse;
import com.api.sisi_yemi.dto.RecentActiveAdResponse;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.Favorite;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.FavoriteRepository;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProfileService profileService;
    private final DynamoDbUtilHelper dynamoDbUtilHelper;

    public List<RecentActiveAdResponse> getUserFavorites(String userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByFavoritedAtDesc(userId);
        DynamoDbHelper<UserAd> userAdsTable = dynamoDbUtilHelper.getUserAdsTable();

        return favorites.stream()
                .map(favorite -> {
                    UserAd ad = userAdsTable.getById(favorite.getAdId())
                            .orElseThrow(() -> new ApiException("Ad not found", HttpStatus.NOT_FOUND, "AD_NOT_FOUND"));
                    ProfileResponse profile = profileService.getProfile(ad.getUserId());

                    return buildAdResponse(ad, profile, favorite.getFavoritedAt());
                })
                .collect(Collectors.toList());
    }

    public RecentActiveAdResponse toggleFavorite(String userId, String adId) {
        Optional<Favorite> existingFavorite = favoriteRepository.findByUserIdAndAdId(userId, adId);
        DynamoDbHelper<UserAd> userAdsTable = dynamoDbUtilHelper.getUserAdsTable();

        UserAd ad = userAdsTable.getById(adId)
                .orElseThrow(() -> new ApiException("Ad not found", HttpStatus.NOT_FOUND, "AD_NOT_FOUND"));
        ProfileResponse profile = profileService.getProfile(ad.getUserId());

        if (existingFavorite.isPresent()) {
            favoriteRepository.deleteByUserIdAndAdId(userId, adId);
            return buildAdResponse(ad, profile, null);
        } else {
            Favorite newFavorite = Favorite.builder()
                    .userId(userId)
                    .adId(ad.getId())
                    .favoritedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            favoriteRepository.save(newFavorite);
            return buildAdResponse(ad, profile, newFavorite.getFavoritedAt());
        }
    }

    private RecentActiveAdResponse buildAdResponse(UserAd ad, ProfileResponse profile, Instant favoritedAt) {
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
                .favoritedAt(favoritedAt)
                .seller(ProfilePreview.builder()
                        .name(profile.getName())
                        .avatarUrl(profile.getAvatarUrl())
                        .rating(profile.getRating())
                        .itemsSold(profile.getItemsSold())
                        .responseRate(profile.getResponseRate())
                        .joinDate(profile.getJoinDate())
                        .build())
                .build();
    }
}
