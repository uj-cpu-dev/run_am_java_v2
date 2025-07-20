package com.api.sisi_yemi.util;

import com.api.sisi_yemi.exception.ApiException;
import com.api.sisi_yemi.model.UserAd;
import com.api.sisi_yemi.repository.UserAdDynamoDbRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdValidator {

    private final UserAdDynamoDbRepository userAdRepository;

    public void validateAdCreation(UserAd userAd) {
        // If creating a draft, skip validation
        if (userAd.getStatus() == UserAd.AdStatus.DRAFT) {
            return;
        }

        // Default to ACTIVE if status is null
        UserAd.AdStatus status = userAd.getStatus() != null
                ? userAd.getStatus()
                : UserAd.AdStatus.ACTIVE;

        if (userAdRepository.existsDuplicate(
                userAd.getUserId(),
                userAd.getTitle(),
                userAd.getPrice(),
                userAd.getCategory(),
                status
        )) {
            throw new ApiException(
                    "Duplicate active ad exists",
                    HttpStatus.BAD_REQUEST,
                    "DUPLICATE_AD"
            );
        }
    }

    public void validateAdUpdate(UserAd updatedAd) {
        if (updatedAd.getStatus() == UserAd.AdStatus.DRAFT) return;

        if (userAdRepository.existsDuplicateExceptId(
                updatedAd.getUserId(),
                updatedAd.getTitle(),
                updatedAd.getPrice(),
                updatedAd.getCategory(),
                updatedAd.getStatus(),
                updatedAd.getId()
        )) {
            throw new ApiException(
                    "Another active ad with the same title, price, and category already exists",
                    HttpStatus.BAD_REQUEST,
                    "DUPLICATE_AD"
            );
        }
    }
}
