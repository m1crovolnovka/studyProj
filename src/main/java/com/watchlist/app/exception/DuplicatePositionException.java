package com.watchlist.app.exception;

public class DuplicatePositionException extends RuntimeException {

	public DuplicatePositionException(String name) {
		super("Position already exists: " + name);
	}
}
