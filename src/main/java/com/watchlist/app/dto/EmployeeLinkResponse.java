package com.watchlist.app.dto;

public record EmployeeLinkResponse(
		Long employeeId,
		Long userId,
		String username,
		String firstName,
		String lastName) {
}