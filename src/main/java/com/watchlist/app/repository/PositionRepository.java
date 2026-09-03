package com.watchlist.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {

	Optional<Position> findByName(String name);

	boolean existsByName(String name);
}
