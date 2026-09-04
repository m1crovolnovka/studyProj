package com.watchlist.app.dto;

import java.time.Instant;

import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.domain.TaskStatus;

public record EmployeeTaskResponse(
		Long id,
		Long employeeId,
		String title,
		String description,
		TaskStatus status,
		Instant createdAt,
		Instant completedAt) {

	public static EmployeeTaskResponse from(EmployeeTask task) {
		return new EmployeeTaskResponse(
				task.getId(),
				task.getEmployee().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getCreatedAt(),
				task.getCompletedAt());
	}
}