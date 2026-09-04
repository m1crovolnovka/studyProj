package com.watchlist.app.dto;

import com.watchlist.app.domain.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record EmployeeTaskStatusUpdateRequest(

		@NotNull TaskStatus status

) {
}