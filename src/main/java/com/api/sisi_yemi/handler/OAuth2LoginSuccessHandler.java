/*package com.api.sisi_yemi.handler;

import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import com.api.sisi_yemi.util.token.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final DynamoDbHelperFactory dynamoDbHelperFactory;

    private final String redirectUri;

    private final String USER_TABLE_NAME;

    public OAuth2LoginSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            DynamoDbHelperFactory dynamoDbHelperFactory,
            @Value("${frontend.redirect-uri}") String redirectUri,
            @Value("${users.table}") String userTableName
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.dynamoDbHelperFactory = dynamoDbHelperFactory;
        this.redirectUri = redirectUri;
        this.USER_TABLE_NAME = userTableName;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        String provider = extractProviderFromRequest(request);

        if (email == null) {
            log.error("OAuth2 success handler failed: email not found in OAuth2 response.");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Email not found in OAuth2 user");
            return;
        }

        DynamoDbHelper<User> userHelper = dynamoDbHelperFactory.getHelper(USER_TABLE_NAME, User.class);

        // Find user by email (DynamoDB does not support querying by non-key without a GSI)
        Optional<User> userOpt = userHelper.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();

        User user = userOpt.orElseGet(() -> {
            log.info("User not found. Creating new user: {}", email);
            User newUser = User.builder()
                    .id(UUID.randomUUID().toString())
                    .email(email)
                    .name(name)
                    .avatarUrl(picture)
                    .provider(provider)
                    .build();
            userHelper.save(newUser);
            return newUser;
        });

        String token = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), Collections.singletonList("ROLE"));

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("email", email)
                .queryParam("name", name)
                .build().toUriString();

        log.info("Redirecting to frontend with token for user: {}", email);
        response.sendRedirect(targetUrl);
    }

    private String extractProviderFromRequest(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("google")) return "google";
        if (referer != null && referer.contains("facebook")) return "facebook";
        return "unknown";
    }
}*/
