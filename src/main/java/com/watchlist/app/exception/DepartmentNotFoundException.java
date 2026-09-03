package com.watchlist.app.exception;

public class DepartmentNotFoundException extends RuntimeException {

	public DepartmentNotFoundException(Long id) {
		super("Department not found: " + id);
	}
}
