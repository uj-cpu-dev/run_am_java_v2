package com.api.sisi_yemi.util;

import com.api.sisi_yemi.model.UserAd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilterAdHelper {

    public static UserAd.AdStatus parseStatus(String statusStr) {
        try {
            return UserAd.AdStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr);
        }
    }

    public static QueryEnhancedRequest buildQueryRequest(UserAd.AdStatus status, String sortDir, Map<String, String> paginationToken) {
        String statusValue = status.name();
        String datePostedValue = paginationToken != null ? paginationToken.get("datePosted") : null;

        Key.Builder keyBuilder = Key.builder().partitionValue(statusValue);
        QueryEnhancedRequest.Builder builder = QueryEnhancedRequest.builder()
                .scanIndexForward("asc".equalsIgnoreCase(sortDir))
                .limit(20);

        // Build QueryConditional based on sort direction
        String sortKey = (datePostedValue != null)
                ? datePostedValue
                : ("desc".equalsIgnoreCase(sortDir)
                ? ZonedDateTime.now().plusYears(10).toString()
                : ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC).toString());

        keyBuilder.sortValue(sortKey);

        builder.queryConditional(
                "desc".equalsIgnoreCase(sortDir)
                        ? QueryConditional.sortLessThanOrEqualTo(keyBuilder.build())
                        : QueryConditional.sortGreaterThanOrEqualTo(keyBuilder.build())
        );

        // Apply exclusiveStartKey for pagination
        if (paginationToken != null) {
            String exclusiveStatus = paginationToken.get("status");
            String exclusiveDate = paginationToken.get("datePosted");

            if (exclusiveStatus != null && exclusiveDate != null) {
                Map<String, AttributeValue> startKey = new HashMap<>();
                startKey.put("status", AttributeValue.fromS(exclusiveStatus));
                startKey.put("datePosted", AttributeValue.fromS(exclusiveDate));
                builder.exclusiveStartKey(startKey);
            } else {
                throw new IllegalArgumentException("Invalid pagination token: missing 'status' or 'datePosted'");
            }
        }

        return builder.build();
    }

    /*public static List<UserAd> filterPageItems(
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
                        (category == null || ad.getCategory() != null &&
                                ad.getCategory().toLowerCase().contains(category.trim().toLowerCase())) &&
                                (location == null || ad.getLocation() != null &&
                                        ad.getLocation().equalsIgnoreCase(location.trim())) &&
                                (condition == null || ad.getCondition() != null &&
                                        ad.getCondition().equalsIgnoreCase(condition.trim())) &&
                                (minPrice == null || ad.getPrice() >= minPrice) &&
                                (maxPrice == null || ad.getPrice() <= maxPrice) &&
                                (search == null || ad.getTitle() != null &&
                                        ad.getTitle().toLowerCase().contains(search.toLowerCase()))
                )
                .collect(Collectors.toList());
    }*/

    public List<UserAd> filterPageItems(List<UserAd> items,
                                         String category,
                                         String location,
                                         String condition,
                                         Double minPrice,
                                         Double maxPrice,
                                         String search) {
        return items.stream()
                .filter(ad -> category == null || matchesCategory(ad, category))
                .filter(ad -> location == null || matchesLocation(ad, location))
                .filter(ad -> condition == null || matchesCondition(ad, condition))
                .filter(ad -> minPrice == null || matchesMinPrice(ad, minPrice))
                .filter(ad -> maxPrice == null || matchesMaxPrice(ad, maxPrice))
                .filter(ad -> search == null || matchesSearch(ad, search))
                .collect(Collectors.toList());
    }
    private boolean matchesSearch(UserAd ad, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        String normalizedSearch = searchTerm.toLowerCase();

        // Search across multiple fields with different weights/priority
        return
                // Exact matches (highest priority)
                (ad.getTitle() != null && ad.getTitle().toLowerCase().equals(normalizedSearch)) ||
                        (ad.getCategory() != null && ad.getCategory().toLowerCase().equals(normalizedSearch)) ||

                        // Partial matches in title (high priority)
                        (ad.getTitle() != null && ad.getTitle().toLowerCase().contains(normalizedSearch)) ||

                        // Partial matches in description (medium priority)
                        (ad.getDescription() != null && ad.getDescription().toLowerCase().contains(normalizedSearch)) ||

                        // Partial matches in category (medium priority)
                        (ad.getCategory() != null && ad.getCategory().toLowerCase().contains(normalizedSearch)) ||

                        // Partial matches in location (low priority)
                        (ad.getLocation() != null && ad.getLocation().toLowerCase().contains(normalizedSearch)) ||

                        // Word-by-word matching for better search results
                        matchesIndividualWords(ad, normalizedSearch);
    }

    // Helper method for word-by-word matching
    private boolean matchesIndividualWords(UserAd ad, String searchTerm) {
        String[] searchWords = searchTerm.split("\\s+");

        for (String word : searchWords) {
            if (word.length() < 2) continue; // Skip very short words

            boolean wordFound =
                    (ad.getTitle() != null && ad.getTitle().toLowerCase().contains(word)) ||
                            (ad.getDescription() != null && ad.getDescription().toLowerCase().contains(word)) ||
                            (ad.getCategory() != null && ad.getCategory().toLowerCase().contains(word)) ||
                            (ad.getLocation() != null && ad.getLocation().toLowerCase().contains(word));

            if (!wordFound) {
                return false;
            }
        }

        return searchWords.length > 0;
    }

    private boolean matchesCategory(UserAd ad, String category) {
        return ad.getCategory() != null &&
                ad.getCategory().toLowerCase().contains(category.toLowerCase());
    }

    private boolean matchesLocation(UserAd ad, String location) {
        return ad.getLocation() != null &&
                ad.getLocation().toLowerCase().contains(location.toLowerCase());
    }

    private boolean matchesCondition(UserAd ad, String condition) {
        return ad.getCondition() != null &&
                ad.getCondition().toLowerCase().equals(condition.toLowerCase());
    }

    private boolean matchesMinPrice(UserAd ad, Double minPrice) {
        return ad.getPrice() >= minPrice;
    }

    private boolean matchesMaxPrice(UserAd ad, Double maxPrice) {
        return ad.getPrice() <= maxPrice;
    }


    /*public static void sortResults(List<UserAd> ads, String sortBy, String sortDir) {
        Comparator<UserAd> comparator = switch (sortBy) {
            case "price" -> Comparator.comparing(UserAd::getPrice);
            case "datePosted" -> Comparator.comparing(UserAd::getDatePosted);
            default -> Comparator.comparing(UserAd::getDatePosted);
        };

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        ads.sort(comparator);
    }*/

    public void sortResults(List<UserAd> ads, String sortBy, String sortDir) {
        if (ads == null || ads.isEmpty()) return;

        Comparator<UserAd> comparator;

        switch (sortBy.toLowerCase()) {
            case "price":
                comparator = Comparator.comparingDouble(UserAd::getPrice);
                break;
            case "title":
                comparator = Comparator.comparing(ad ->
                        ad.getTitle() != null ? ad.getTitle().toLowerCase() : "");
                break;
            case "location":
                comparator = Comparator.comparing(ad ->
                        ad.getLocation() != null ? ad.getLocation().toLowerCase() : "");
                break;
            case "dateposted":
            default:
                comparator = Comparator.comparing(UserAd::getDatePosted);
                break;
        }

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
