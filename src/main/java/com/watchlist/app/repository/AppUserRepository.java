package com.watchlist.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);
}