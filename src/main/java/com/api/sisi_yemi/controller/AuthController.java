package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.AuthResponse;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.token.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final DynamoDbUtilHelper dynamoDbUtilHelper;

    public AuthController(JwtTokenProvider jwtTokenProvider, DynamoDbUtilHelper dynamoDbUtilHelper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.dynamoDbUtilHelper = dynamoDbUtilHelper;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        DynamoDbHelper<User> userHelper = dynamoDbUtilHelper.getUserTable();
        User user = userHelper.getById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), List.of("ROLE_USER"));

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }
}
