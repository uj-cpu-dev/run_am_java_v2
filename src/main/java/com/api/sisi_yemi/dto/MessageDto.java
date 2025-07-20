package com.api.sisi_yemi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDto {
    private String id;
    private String conversationId;
    private UserDto sender;
    private String content;
    private LocalDateTime timestamp;
    private String status;
    private int unread;
}
