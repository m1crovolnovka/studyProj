package com.watchlist.app.dto;

import java.math.BigDecimal;

import com.watchlist.app.domain.Employee;

public record EmployeeResponse(
		Long id,
		Long departmentId,
		String firstName,
		String lastName,
		String email,
		PositionResponse position,
		BigDecimal baseSalary,
		BigDecimal salary) {

	public static EmployeeResponse from(Employee employee) {
		return new EmployeeResponse(
				employee.getId(),
				employee.getDepartment().getId(),
				employee.getFirstName(),
				employee.getLastName(),
				employee.getEmail(),
				employee.getPosition() != null ? PositionResponse.from(employee.getPosition()) : null,
				employee.getBaseSalary(),
				employee.getSalary());
	}
}
