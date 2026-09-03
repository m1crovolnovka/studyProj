package com.watchlist.app.dto;

import com.watchlist.app.domain.Department;

public record DepartmentResponse(
		Long id,
		String name,
		String location) {

	public static DepartmentResponse from(Department department) {
		return new DepartmentResponse(
				department.getId(),
				department.getName(),
				department.getLocation());
	}
}
