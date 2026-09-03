package com.watchlist.app.exception;

public class ReviewNotFoundException extends RuntimeException {

	public ReviewNotFoundException(Long id) {
		super("Review not found: " + id);
	}
}
