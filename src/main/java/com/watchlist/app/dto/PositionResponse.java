package com.watchlist.app.dto;

import java.math.BigDecimal;

import com.watchlist.app.domain.Position;

public record PositionResponse(
		Long id,
		String name,
		BigDecimal coefficient) {

	public static PositionResponse from(Position position) {
		return new PositionResponse(position.getId(), position.getName(), position.getCoefficient());
	}
}
