package com.watchlist.app.dto;

import java.time.LocalDateTime;

import com.watchlist.app.domain.Review;

public record ReviewResponse(
		Long id,
		Long titleId,
		String content,
		Integer rating,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static ReviewResponse from(Review review) {
		return new ReviewResponse(
				review.getId(),
				review.getTitle().getId(),
				review.getContent(),
				review.getRating(),
				review.getCreatedAt(),
				review.getUpdatedAt());
	}
}
