package com.api.sisi_yemi.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProfilePreview {
    private String name;
    private String avatarUrl;
    private Double rating;
    private Integer itemsSold;
    private Double responseRate;
    private String joinDate;
}

