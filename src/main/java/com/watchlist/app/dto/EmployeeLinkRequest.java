package com.watchlist.app.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeLinkRequest(

		@NotNull Long userId

) {
}