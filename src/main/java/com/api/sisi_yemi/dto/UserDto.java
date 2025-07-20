package com.api.sisi_yemi.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String name;
    private String avatar;
    private boolean verified;
    private double rating;
}
