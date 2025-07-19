package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.UserLoginRequest;
import com.api.sisi_yemi.dto.UserSignupRequest;
import com.api.sisi_yemi.dto.UserUpdateRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final DynamoDbHelperFactory dynamoDbHelperFactory;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationTokenService verificationTokenService;

    @Value("${users.table}")
    private final String USER_TABLE_NAME;

    public void register(UserSignupRequest request) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        List<User> existingUsers = userHelper.queryByGsi("email-index", "email", request.getEmail());
        if (!existingUsers.isEmpty()) {
            throw new ApiException("Email already in use", HttpStatus.BAD_REQUEST, "EMAIL_ALREADY_IN_USE");
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .joinDate(Instant.now())
                .enabled(false)
                .build();

        String token = UUID.randomUUID().toString();

        try {
            emailService.sendVerificationEmail(user.getEmail(), token);

            userHelper.save(user);
            verificationTokenService.saveToken(token, user.getId());

        } catch (MailException e) {
            throw new ApiException("Failed to send verification email", HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED");
        }
    }

    public User login(UserLoginRequest request) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        List<User> users = userHelper.queryByGsi("email-index", "email", request.getEmail());
        if (users.isEmpty()) {
            throw new ApiException("User not found", HttpStatus.BAD_REQUEST, "USER_NOT_FOUND");
        }

        User user = users.get(0);

        if (!user.isEnabled()) {
            throw new ApiException("Email not verified", HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        return user;
    }

    public void updateUser(String userId, UserUpdateRequest request) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        User user = userHelper.getById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.BAD_REQUEST, "USER_NOT_FOUND"));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new ApiException("Current password is incorrect", HttpStatus.UNAUTHORIZED, "INVALID_CURRENT_PASSWORD");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            List<User> usersWithEmail = userHelper.queryByGsi("email-index", "email", request.getEmail());
            if (!usersWithEmail.isEmpty()) {
                throw new ApiException("New email already in use", HttpStatus.UNAUTHORIZED, "NEW_EMAIL_ALREADY_IN_USE");
            }
            user.setEmail(request.getEmail());
        }

        userHelper.save(user);
    }

    public void resendVerificationEmail(String email) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        List<User> users = userHelper.queryByGsi("email-index", "email", email);
        if (users.isEmpty()) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
        }

        User user = users.get(0);

        if (user.isEnabled()) {
            throw new ApiException("User already verified", HttpStatus.BAD_REQUEST, "USER_ALREADY_VERIFIED");
        }

        String token = UUID.randomUUID().toString();

        try {
            emailService.sendVerificationEmail(user.getEmail(), token);
            verificationTokenService.deleteById(user.getId());
            verificationTokenService.saveToken(token, user.getId());

        } catch (MailException e) {
            throw new ApiException("Failed to send verification email", HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_SEND_FAILED");
        }
    }
}
