package com.watchlist.app.exception;

public class DuplicateDepartmentException extends RuntimeException {

	public DuplicateDepartmentException(String name) {
		super("Department already exists: " + name);
	}
}
