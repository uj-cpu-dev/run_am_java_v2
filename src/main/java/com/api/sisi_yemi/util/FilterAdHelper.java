package com.api.sisi_yemi.util;

import com.api.sisi_yemi.model.UserAd;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterAdHelper {

    public static UserAd.AdStatus parseStatus(String statusStr) {
        try {
            return UserAd.AdStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }
    }

    public static QueryEnhancedRequest buildQueryRequest(UserAd.AdStatus status, String sortDir, Map<String, String> paginationToken) {
        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(status.name()).build())) // Use name() instead of ordinal()
                .scanIndexForward("asc".equalsIgnoreCase(sortDir))
                .limit(20);

        if (paginationToken != null && !paginationToken.isEmpty()) {
            Map<String, AttributeValue> startKey = new HashMap<>();

            if (paginationToken.containsKey("status")) {
                // Store as string to match GSI definition
                startKey.put("status", AttributeValue.fromS(paginationToken.get("status")));
            }

            if (paginationToken.containsKey("datePosted")) {
                startKey.put("datePosted", AttributeValue.fromS(paginationToken.get("datePosted")));
            }

            builder.exclusiveStartKey(startKey);
        }

        return builder.build();
    }

    public static List<UserAd> filterPageItems(
            List<UserAd> items,
            String category,
            String location,
            String condition,
            Double minPrice,
            Double maxPrice,
            String search
    ) {
        return items.stream()
                .filter(ad ->
                        (category == null || category.equalsIgnoreCase(ad.getCategory())) &&
                                (location == null || location.equalsIgnoreCase(ad.getLocation())) &&
                                (condition == null || condition.equalsIgnoreCase(ad.getCondition())) &&
                                (minPrice == null || ad.getPrice() >= minPrice) &&
                                (maxPrice == null || ad.getPrice() <= maxPrice) &&
                                (search == null || ad.getTitle().toLowerCase().contains(search.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    public static void sortResults(List<UserAd> ads, String sortBy, String sortDir) {
        Comparator<UserAd> comparator = switch (sortBy) {
            case "price" -> Comparator.comparing(UserAd::getPrice);
            case "datePosted" -> Comparator.comparing(UserAd::getDatePosted);
            default -> Comparator.comparing(UserAd::getDatePosted);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        ads.sort(comparator);
    }

    public static Map<String, String> buildPaginationToken(Map<String, AttributeValue> lastEvaluatedKey) {
        if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) return null;

        Map<String, String> token = new HashMap<>();
        if (lastEvaluatedKey.containsKey("status") && lastEvaluatedKey.get("status").s() != null) {
            token.put("status", lastEvaluatedKey.get("status").s()); // Get as string
        }
        if (lastEvaluatedKey.containsKey("datePosted") && lastEvaluatedKey.get("datePosted").s() != null) {
            token.put("datePosted", lastEvaluatedKey.get("datePosted").s());
        }
        return token;
    }
}
