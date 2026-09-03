package com.watchlist.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.RefreshToken;
import com.watchlist.app.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenLifetime;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-lifetime}") Duration refreshTokenLifetime
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    @Transactional
    public String create(AppUser user) {

        String rawToken = generateToken();

        RefreshToken refreshToken = new RefreshToken(
                hash(rawToken),
                user,
                Instant.now().plus(refreshTokenLifetime)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {

        RefreshToken current = refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if (current.isRevoked() || current.isExpired()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        current.revoke();

        String newRawToken = generateToken();

        RefreshToken replacement = new RefreshToken(
                hash(newRawToken),
                current.getUser(),
                Instant.now().plus(refreshTokenLifetime)
        );

        refreshTokenRepository.save(replacement);

        return new RotatedRefreshToken(
                newRawToken,
                replacement
        );
    }

    @Transactional
    public void revoke(String rawToken) {

        refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    private String generateToken() {

        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    e
            );
        }
    }
}