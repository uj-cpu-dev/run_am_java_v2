package com.api.sisi_yemi.dto;

import com.api.sisi_yemi.model.UserAd;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdDetailsResponse {
    // Ad fields
    private String id;
    private String title;
    private double price;
    private String category;
    private String description;
    private String location;
    private String condition;
    private List<UserAd.ImageData> images;
    private int views;
    private int messages;
    private Instant datePosted;
    private UserAd.AdStatus status;
    private Instant dateSold;

    // Seller fields
    private String sellerName;
    private String sellerAvatarUrl;
    private double sellerRating;
    private int sellerItemsSold;
    private double sellerResponseRate;
    private String sellerJoinDate;
    private double sellerReviews;
    // Add other seller fields as needed
}
