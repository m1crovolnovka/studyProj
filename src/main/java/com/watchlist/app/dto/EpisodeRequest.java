package com.watchlist.app.dto;

import com.watchlist.app.domain.EpisodeStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EpisodeRequest(
		@NotNull @Min(1) Integer seasonNumber,
		@NotNull @Min(1) Integer episodeNumber,
		@NotBlank @Size(max = 255) String name,
		EpisodeStatus episodeStatus,
		@Min(1) @Max(10) Integer rating,
		@Size(max = 2000) String notes) {
}
