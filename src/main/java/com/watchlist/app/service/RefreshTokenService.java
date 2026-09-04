package com.watchlist.app.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.RefreshToken;
import com.watchlist.app.repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {

    private static final Logger log =
            LoggerFactory.getLogger(RefreshTokenService.class);

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
                .orElseThrow(() -> {
                    log.warn("Refresh token rejected: unknown token hash {}",
                            mask(hash(rawToken)));
                    return new IllegalArgumentException(
                            "Refresh token is invalid or has already been used");
                });

        if (current.isRevoked()) {
            log.warn("Refresh token rejected: token has been revoked (userId={})",
                    current.getUser().getId());
            throw new IllegalArgumentException(
                    "Refresh token has been revoked");
        }

        if (current.isExpired()) {
            log.warn("Refresh token rejected: token has expired (userId={})",
                    current.getUser().getId());
            throw new IllegalArgumentException(
                    "Refresh token has expired");
        }

        AppUser user = current.getUser();

        // user is a lazy proxy here; initialize it while the
        // transaction (and thus the Hibernate session) is still open,
        // so the caller can safely use it after the transaction ends.
        Hibernate.initialize(user);

        current.revoke();

        String newRawToken = generateToken();

        RefreshToken replacement = new RefreshToken(
                hash(newRawToken),
                user,
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

    private String mask(String tokenHash) {
        return tokenHash.substring(0, Math.min(8, tokenHash.length()))
                + "...";
    }
}