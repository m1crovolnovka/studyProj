package com.watchlist.app.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PositionRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @DecimalMin("0.0") BigDecimal coefficient) {
}
