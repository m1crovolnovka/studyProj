package com.watchlist.app.dto;

import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;

public record TitleResponse(
		Long id,
		String name,
		TitleType type,
		Integer releaseYear,
		String genre,
		WatchStatus watchStatus,
		Integer rating,
		String notes) {

	public static TitleResponse from(Title title) {
		return new TitleResponse(
				title.getId(),
				title.getName(),
				title.getType(),
				title.getReleaseYear(),
				title.getGenre(),
				title.getWatchStatus(),
				title.getRating(),
				title.getNotes());
	}
}
