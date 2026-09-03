package com.watchlist.app.exception;

public class TitleNotFoundException extends RuntimeException {

	public TitleNotFoundException(Long id) {
		super("Title not found: " + id);
	}
}
