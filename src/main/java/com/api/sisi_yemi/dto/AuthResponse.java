package com.api.sisi_yemi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String avatarUrl;
    private String email;
    private String name;
    private String message;
}
