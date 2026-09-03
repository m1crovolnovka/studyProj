package com.watchlist.app.service;

import com.watchlist.app.domain.RefreshToken;

public record RotatedRefreshToken(
        String rawToken,
        RefreshToken token
) {
}