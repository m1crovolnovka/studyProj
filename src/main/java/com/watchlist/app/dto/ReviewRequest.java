package com.watchlist.app.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
		@NotBlank @Size(max = 4000) String content,
		@Min(1) @Max(10) Integer rating) {
}
