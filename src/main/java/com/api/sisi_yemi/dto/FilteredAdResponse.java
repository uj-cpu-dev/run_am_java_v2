package com.api.sisi_yemi.dto;

import com.api.sisi_yemi.model.UserAd;

import java.util.List;
import java.util.Map;

public record FilteredAdResponse(
        List<UserAd> items,
        Map<String, String> nextToken,
        boolean hasNextPage
) {}
