package com.watchlist.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmployeeTaskRequest(

		@NotBlank @Size(max = 200) String title,

		@Size(max = 500) String description

) {
}