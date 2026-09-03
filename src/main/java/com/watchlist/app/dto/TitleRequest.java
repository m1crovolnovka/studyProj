package com.watchlist.app.dto;

import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TitleRequest(
		@NotBlank @Size(max = 255) String name,
		@NotNull TitleType type,
		@Min(1870) @Max(2100) Integer releaseYear,
		@Size(max = 100) String genre,
		WatchStatus watchStatus,
		@Min(1) @Max(10) Integer rating,
		@Size(max = 2000) String notes) {
}
