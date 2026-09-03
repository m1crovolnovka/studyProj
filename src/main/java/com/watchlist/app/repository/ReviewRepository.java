package com.watchlist.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByTitleIdOrderByCreatedAtDesc(Long titleId);

	long countByTitleId(Long titleId);
}
