package com.watchlist.app.exception;

public class PositionNotFoundException extends RuntimeException {

	public PositionNotFoundException(Long id) {
		super("Position not found: " + id);
	}
}
