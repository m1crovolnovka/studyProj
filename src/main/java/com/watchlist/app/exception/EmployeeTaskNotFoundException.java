package com.watchlist.app.exception;

public class EmployeeTaskNotFoundException extends RuntimeException {

	public EmployeeTaskNotFoundException(Long id) {
		super("Employee task not found: " + id);
	}
}