package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.Favorite;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FavoriteRepository {

    private final DynamoDbUtilHelper dynamoDbUtilHelper;
    private static final String USER_AD_INDEX = "userId-adId-index";
    private static final String USER_INDEX = "userId-index";

    public Optional<Favorite> findByUserIdAndAdId(String userId, String adId) {
        List<Favorite> results = dynamoDbUtilHelper.getFavoriteTable()
                .queryByCompositeGsi(USER_AD_INDEX, userId, adId);

        return results.stream().findFirst();
    }

    public void deleteByUserIdAndAdId(String userId, String adId) {
        findByUserIdAndAdId(userId, adId)
                .ifPresent(fav -> dynamoDbUtilHelper.getFavoriteTable().deleteById(fav.getId()));
    }

    public List<Favorite> findByUserIdOrderByFavoritedAtDesc(String userId) {
        List<Favorite> favorites = dynamoDbUtilHelper.getFavoriteTable()
                .queryByGsi(USER_INDEX, "userId", userId);

        return favorites.stream()
                .sorted(Comparator.comparing(Favorite::getFavoritedAt).reversed())
                .collect(Collectors.toList());
    }

    public void save(Favorite favorite) {
        if (favorite.getId() == null) {
            favorite.setId(UUID.randomUUID().toString());
        }
        dynamoDbUtilHelper.getFavoriteTable().save(favorite);
    }
}
