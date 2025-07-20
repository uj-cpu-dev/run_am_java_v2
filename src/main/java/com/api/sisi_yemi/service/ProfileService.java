package com.api.sisi_yemi.service;

import com.api.sisi_yemi.dto.ProfileResponse;
import com.api.sisi_yemi.dto.UpdateProfileRequest;
import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepository;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ProfileService {

    private final DynamoDbHelperFactory dynamoDbHelperFactory;
    private final UserAdDynamoDbRepository userAdRepository;
    private final String USER_TABLE_NAME;

    public ProfileService(DynamoDbHelperFactory dynamoDbHelperFactory,
                          UserAdDynamoDbRepository userAdRepository,
                          UserAdService userAdService,
                          @Value("${users.table}") String USER_TABLE_NAME) {
        this.dynamoDbHelperFactory = dynamoDbHelperFactory;
        this.userAdRepository = userAdRepository;
        this.USER_TABLE_NAME = USER_TABLE_NAME;
    }

    public ProfileResponse getProfile(String userId) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        User user = userHelper.getById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.BAD_REQUEST, "USER_NOT_FOUND"));

        List<UserAd> userAds = userAdRepository.findByUserId(userId);

        return ProfileResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .location(user.getLocation())
                .bio(user.getBio())
                .joinDate(formatJoinDate(user.getJoinDate()))
                .avatarUrl(user.getAvatarUrl())
                .rating(user.getRating())
                .itemsSold(user.getItemsSold())
                .activeListings((int) userAds.stream()
                        .filter(ad -> ad.getStatus() == UserAd.AdStatus.ACTIVE)
                        .count())
                .responseRate(user.getResponseRate())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .build();

    }

    public void updateProfile(String userId, UpdateProfileRequest request) {
        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        User user = userHelper.getById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.BAD_REQUEST, "USER_NOT_FOUND"));

        user.setAvatarUrl(request.getAvatarUrl());

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (!userHelper.queryByGsi("email-index", "email", request.getEmail()).isEmpty()) {
                throw new ApiException("New email already in use", HttpStatus.UNAUTHORIZED, "NEW_EMAIL_ALREADY_IN_USE");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
            user.setPhoneVerified(false);
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }


        userHelper.save(user);
    }

    public String formatJoinDate(Instant joinDate) {
        if (joinDate == null) {
            return "N/A";
        }
        return DateTimeFormatter.ofPattern("dd MMM yyyy")
                .withZone(ZoneId.systemDefault())
                .format(joinDate);
    }
}
