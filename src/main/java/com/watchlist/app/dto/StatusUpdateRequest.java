package com.watchlist.app.dto;

import com.watchlist.app.domain.WatchStatus;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull WatchStatus watchStatus) {
}
