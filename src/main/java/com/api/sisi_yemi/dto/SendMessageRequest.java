package com.api.sisi_yemi.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String content;
    private String attachmentUrl;
}
