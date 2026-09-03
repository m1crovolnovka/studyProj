package com.watchlist.app.dto;

import com.watchlist.app.domain.Episode;
import com.watchlist.app.domain.EpisodeStatus;

public record EpisodeResponse(
		Long id,
		Long titleId,
		Integer seasonNumber,
		Integer episodeNumber,
		String name,
		EpisodeStatus episodeStatus,
		Integer rating,
		String notes) {

	public static EpisodeResponse from(Episode episode) {
		return new EpisodeResponse(
				episode.getId(),
				episode.getTitle().getId(),
				episode.getSeasonNumber(),
				episode.getEpisodeNumber(),
				episode.getName(),
				episode.getEpisodeStatus(),
				episode.getRating(),
				episode.getNotes());
	}
}
