package com.api.sisi_yemi.dto;

import lombok.Data;

@Data
public class ItemDto {
    private String id;
    private String title;
    private double price;
    private String image;
    // No owner/seller reference
}
