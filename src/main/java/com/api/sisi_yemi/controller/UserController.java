package com.api.sisi_yemi.controller;

import com.api.sisi_yemi.dto.*;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.model.VerificationToken;
import com.api.sisi_yemi.service.UserService;
import com.api.sisi_yemi.service.VerificationTokenService;
import com.api.sisi_yemi.util.auth.AuthenticationHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import com.api.sisi_yemi.util.token.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final VerificationTokenService verificationTokenService;
    private final AuthenticationHelper authHelper;
    private final JwtTokenProvider jwtTokenProvider;
    private final DynamoDbHelperFactory dynamoDbHelperFactory;

    private final String USER_TABLE_NAME;

    public UserController(UserService userService,
                          VerificationTokenService verificationTokenService,
                          AuthenticationHelper authHelper,
                          JwtTokenProvider jwtTokenProvider,
                          DynamoDbHelperFactory dynamoDbHelperFactory,
                          @Value("${users.table}") String USER_TABLE_NAME) {
        this.userService = userService;
        this.verificationTokenService = verificationTokenService;
        this.authHelper = authHelper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.dynamoDbHelperFactory = dynamoDbHelperFactory;
        this.USER_TABLE_NAME = USER_TABLE_NAME;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token, HttpServletResponse response) {
        VerificationToken verificationToken = verificationTokenService.findByToken(token)
                .orElseThrow(() -> new ApiException("Invalid verification token", HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message("Your verification link has expired.")
                            .build());
        }

        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        User user = userHelper.getById(verificationToken.getUserId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        user.setEnabled(true);
        userHelper.save(user);
        verificationTokenService.deleteById(verificationToken.getId());

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), List.of("ROLE_USER"));
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody UserSignupRequest request) {
        userService.register(request);
        return ApiResponse.create("User registered successfully", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody UserLoginRequest request, HttpServletResponse response) {
        User user = userService.login(request);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), List.of("ROLE_USER"));
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build());
    }



    @PutMapping("/update-user")
    public ResponseEntity<ApiResponse> updateUser(@RequestBody UserUpdateRequest request) {
        String userId = authHelper.getAuthenticatedUserId();
        userService.updateUser(userId, request);
        return ApiResponse.create("User updated successfully", HttpStatus.OK);
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<ApiResponse> deleteUser() {
        String userId = authHelper.getAuthenticatedUserId();
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);
        userHelper.deleteById(userId);
        return ApiResponse.create("User has been deleted", HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerification(@RequestBody ResendRequest request) {
        userService.resendVerificationEmail(request.getEmail());
        return ApiResponse.create("Verification email resent", HttpStatus.OK);
    }
}