package com.api.sisi_yemi.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ErrorResponse {
    private String message;
    private String errorCode;
    private HttpStatus status;
}
