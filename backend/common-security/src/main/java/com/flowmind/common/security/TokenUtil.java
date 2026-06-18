package com.flowmind.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public final class TokenUtil {
    private static final String PREFIX = "mock-jwt.";

    private TokenUtil() {
    }

    public static String createMockToken(String username) {
        String raw = username + ":" + Instant.now().plusSeconds(86400).getEpochSecond();
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isValid(String authorization) {
        return parseUsername(authorization).isPresent();
    }

    public static Optional<String> parseUsername(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer " + PREFIX)) {
            return Optional.empty();
        }
        String token = authorization.substring("Bearer ".length());
        String payload = token.substring(PREFIX.length());

        // Backward compatibility for old frontend/mobile demo tokens: mock-jwt.demo or mock-jwt.admin.
        if (!payload.contains(".") && !payload.contains(":") && !looksLikeBase64(payload)) {
            return Optional.of(payload.isBlank() ? "admin" : payload);
        }

        try {
            String raw = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 2);
            if (parts.length < 2 || parts[0].isBlank()) {
                return Optional.empty();
            }
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(parts[0]);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static boolean looksLikeBase64(String value) {
        return value.matches("[A-Za-z0-9_-]{12,}");
    }
}
