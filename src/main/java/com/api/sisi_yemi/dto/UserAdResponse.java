package com.api.sisi_yemi.dto;

import com.api.sisi_yemi.model.UserAd;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class UserAdResponse {
    private String id;
    private String title;
    private double price;
    private List<UserAd.ImageData> images;
    private int views;
    private int messages;
    private Instant datePosted;
    private Instant dateSold;
    private UserAd.AdStatus status;
    private String category;
    private String location;
    private String condition;
    private String description;

    public static UserAdResponse fromEntity(UserAd userAd) {
        UserAdResponse response = new UserAdResponse();
        response.setId(userAd.getId());
        response.setTitle(userAd.getTitle());
        response.setPrice(userAd.getPrice());
        response.setImages(userAd.getImages());
        response.setViews(userAd.getViews());
        response.setMessages(userAd.getMessages());
        response.setDatePosted(userAd.getDatePosted());
        response.setDateSold(userAd.getDateSold());
        response.setStatus(userAd.getStatus());
        response.setCategory(userAd.getCategory());
        response.setLocation(userAd.getLocation());
        response.setCondition(userAd.getCondition());
        response.setDescription(userAd.getDescription());
        return response;
    }
}
