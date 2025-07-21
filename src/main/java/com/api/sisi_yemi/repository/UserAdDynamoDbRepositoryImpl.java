package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.UserAd;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserAdDynamoDbRepositoryImpl implements UserAdDynamoDbRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<UserAd> table() {
        return enhancedClient.table("user_ads", TableSchema.fromBean(UserAd.class));
    }

    @Override
    public void save(UserAd ad) {
        ad.setDatePosted(Optional.ofNullable(ad.getDatePosted()).orElse(Instant.now()));
        ad.computeDedupeKey();
        table().putItem(ad);
    }

    @Override
    public Optional<UserAd> findById(String id) {
        return Optional.ofNullable(table().getItem(Key.builder().partitionValue(id).build()));
    }

    @Override
    @Cacheable("userAds")
    public List<UserAd> findByUserId(String userId) {
        return queryByIndex("userId-index", userId, null);
    }

    @Override
    public List<UserAd> findByUserIdAndStatus(String userId, UserAd.AdStatus status) {
        DynamoDbIndex<UserAd> index = table().index("userId-status-index");

        return index.query(r -> r
                        .queryConditional(QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(userId).build()))
                        .filterExpression(Expression.builder()
                                .expression("#status = :statusVal")
                                .expressionNames(Map.of("#status", "status"))
                                .expressionValues(Map.of(
                                        ":statusVal", AttributeValue.builder().s(status.name()).build()))
                                .build()))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<UserAd> findRecentActiveAds(int limit) {
        DynamoDbIndex<UserAd> index = table().index("status-datePosted-index");

        return index.query(r -> r
                        .queryConditional(QueryConditional.keyEqualTo(
                                Key.builder()
                                        .partitionValue(UserAd.AdStatus.ACTIVE.name())
                                        .build()))
                        .scanIndexForward(false)
                        .limit(limit))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(String userId) {
        return findByUserId(userId).size();
    }

    @Override
    public long countByUserIdAndStatus(String userId, UserAd.AdStatus status) {
        return findByUserIdAndStatus(userId, status).size();
    }

    @Override
    public boolean existsDuplicate(String userId, String title, double price,
                                   String category, UserAd.AdStatus status) {
        String dedupeKey = userId + "#" + title + "#" + price + "#" + category + "#" + status.name();
        DynamoDbIndex<UserAd> index = table().index("dedupe-index");

        return index.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(dedupeKey).build())))
                .stream()
                .findFirst()
                .map(page -> !page.items().isEmpty())
                .orElse(false);
    }

    @Override
    public boolean existsDuplicateExceptId(String userId, String title, double price,
                                           String category, UserAd.AdStatus status,
                                           String excludedId) {
        String dedupeKey = userId + "#" + title + "#" + price + "#" + category + "#" + status.name();
        DynamoDbIndex<UserAd> index = table().index("dedupe-index");

        return index.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(
                                Key.builder().partitionValue(dedupeKey).build())))
                .stream()
                .flatMap(page -> page.items().stream())
                .anyMatch(ad -> !ad.getId().equals(excludedId));
    }

    @Override
    public List<UserAd> findByStatus(String status, int limit, String lastEvaluatedId) {
        DynamoDbIndex<UserAd> index = table().index("status-datePosted-index");

        QueryEnhancedRequest.Builder requestBuilder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(UserAd.AdStatus.valueOf(status).name()).build()))
                .limit(limit)
                .scanIndexForward(false); // most recent first

        if (lastEvaluatedId != null) {
            // Assuming lastEvaluatedId is the actual datePosted string value
            requestBuilder.exclusiveStartKey(Map.of(
                    "statusForDateIndex", AttributeValue.builder().s(status).build(),
                    "datePosted", AttributeValue.builder().s(lastEvaluatedId).build()
            ));
        }

        // Process paginated results
        return index.query(requestBuilder.build())
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    private List<UserAd> queryByIndex(String indexName, String partitionValue, String sortKeyValue) {
        DynamoDbIndex<UserAd> index = table().index(indexName);

        QueryConditional condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(partitionValue).build());

        return index.query(r -> r.queryConditional(condition))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        table().deleteItem(Key.builder().partitionValue(id).build());
    }
}
