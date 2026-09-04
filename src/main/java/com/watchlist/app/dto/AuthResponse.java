package com.watchlist.app.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {
}