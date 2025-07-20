package com.api.sisi_yemi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 100)
    private String name;

    @Email
    private String email;

    @Pattern(regexp = "^\\+?[0-9\\s-]+$")
    private String phone;

    private String location;
    private String bio;
    private String avatarUrl;
}

