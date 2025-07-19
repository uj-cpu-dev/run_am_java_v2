package com.api.sisi_yemi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@AllArgsConstructor
public class ApiResponse {

    private String message;
    private HttpStatus status;

    public static ResponseEntity<ApiResponse> create(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiResponse(message, status));
    }
}

