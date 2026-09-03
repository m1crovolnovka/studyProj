package com.watchlist.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteAllByUserId(Long userId);
}