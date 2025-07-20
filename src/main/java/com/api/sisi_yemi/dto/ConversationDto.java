package com.api.sisi_yemi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDto {
    private String id;
    private UserDto participant; // Only shows buyer info
    private ItemDto item;
    private String lastMessage;
    private LocalDateTime timestamp;
    private int unread;
    private String status;
    // No seller field exposed
}
