package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.UserAd;

import java.util.List;
import java.util.Optional;

public interface UserAdDynamoDbRepository {
    void save(UserAd ad);

    Optional<UserAd> findById(String id);

    List<UserAd> findByUserId(String userId);

    List<UserAd> findByUserIdAndStatus(String userId, UserAd.AdStatus status);

    List<UserAd> findRecentActiveAds(int limit);

    long countByUserId(String userId);

    long countByUserIdAndStatus(String userId, UserAd.AdStatus status);

    boolean existsDuplicate(String userId, String title, double price, String category, UserAd.AdStatus status);

    boolean existsDuplicateExceptId(String userId, String title, double price, String category, UserAd.AdStatus status, String excludedId);

    List<UserAd> findByStatus(String status, int limit, String lastEvaluatedId);

    void deleteById(String id);
}
