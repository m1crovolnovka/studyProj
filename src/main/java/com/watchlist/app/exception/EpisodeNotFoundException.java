package com.watchlist.app.exception;

public class EpisodeNotFoundException extends RuntimeException {

	public EpisodeNotFoundException(Long id) {
		super("Episode not found: " + id);
	}
}
