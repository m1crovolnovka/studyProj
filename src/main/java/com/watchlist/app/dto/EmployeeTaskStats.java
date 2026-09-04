package com.watchlist.app.dto;

public record EmployeeTaskStats(
		long total,
		long pending,
		long inProgress,
		long completed) {
}