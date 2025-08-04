package com.api.sisi_yemi.util.token;

import com.api.sisi_yemi.util.auth.UserPrincipal;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;

public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthChannelInterceptor.class);
    private final JwtTokenProvider jwtTokenProvider;

    public AuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            logger.debug("🔌 STOMP CONNECT command received");

            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders.isEmpty()) {
                logger.warn("❌ No Authorization header found in STOMP CONNECT");
                throw new JwtException("No JWT token found");
            }

            String token = authHeaders.get(0);
            logger.debug("🪪 Raw token from header: {}", token);

            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
                logger.debug("🔍 Stripped Bearer token: {}", token);

                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String userId = jwtTokenProvider.getUserIdFromToken(token);
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                        logger.debug("✅ Token is valid. User ID: {}, Email: {}, Roles: {}", userId, email, roles);

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                new UserPrincipal(userId, email, roles),
                                null,
                                AuthorityUtils.createAuthorityList(roles.toArray(new String[0]))
                        );
                        accessor.setUser(auth);
                    } else {
                        logger.warn("❌ Token validation failed");
                        throw new JwtException("Invalid JWT token");
                    }
                } catch (Exception e) {
                    logger.error("❗ Error parsing JWT token", e);
                    throw new JwtException("Token parsing error");
                }
            } else {
                logger.warn("❌ Token does not start with 'Bearer '");
                throw new JwtException("Invalid token format");
            }
        }

        return message;
    }
}
