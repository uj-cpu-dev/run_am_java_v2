package com.api.sisi_yemi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {

    private String name;
    private String email;
    private String phone;
    private String location;
    private String bio;
    private String joinDate;
    private String avatarUrl;
    private double rating;
    private int itemsSold;
    private int activeListings;
    private double responseRate;
    private boolean emailVerified;
    private boolean phoneVerified;
    private double reviews;
    private boolean isQuickResponder;
    private boolean isTopSeller;
}

