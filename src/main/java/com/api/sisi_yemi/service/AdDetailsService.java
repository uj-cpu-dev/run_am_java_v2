package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.AdDetailsResponse;
import com.api.sisi_yemi.dto.ProfileResponse;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdDetailsService {
    private final UserAdDynamoDbRepository userAdRepository;
    private final ProfileService profileService;

    public AdDetailsResponse getAdDetails (String adId) {
        UserAd ad = userAdRepository.findById(adId)
                .orElseThrow(() -> new ApiException("Ad not found", HttpStatus.NOT_FOUND, "AD_NOT_FOUND"));

        ProfileResponse sellerProfile = profileService.getProfile(ad.getUserId());

        return AdDetailsResponse.builder()
                // Ad fields
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
                // Seller fields
                .sellerName(sellerProfile.getName())
                .sellerAvatarUrl(sellerProfile.getAvatarUrl())
                .sellerRating(sellerProfile.getRating())
                .sellerItemsSold(sellerProfile.getItemsSold())
                .sellerResponseRate(sellerProfile.getResponseRate())
                .sellerJoinDate(sellerProfile.getJoinDate())
                .sellerReviews(sellerProfile.getReviews())
                .build();
    };
}
