package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.domain.TaskStatus;
import com.watchlist.app.dto.EmployeeTaskRequest;
import com.watchlist.app.dto.EmployeeTaskResponse;
import com.watchlist.app.dto.EmployeeTaskStats;
import com.watchlist.app.service.EmployeeTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees/{employeeId}/tasks")
@Tag(name = "Employee Tasks", description = "Админские операции с заданиями работника")
public class AdminEmployeeTaskController {

	private final EmployeeTaskService employeeTaskService;

	public AdminEmployeeTaskController(EmployeeTaskService employeeTaskService) {
		this.employeeTaskService = employeeTaskService;
	}

	@GetMapping
	@Operation(summary = "Задания работника (только админ)")
	public List<EmployeeTaskResponse> findAll(
			@PathVariable Long employeeId,
			@RequestParam(required = false) TaskStatus status) {
		return employeeTaskService.findByEmployeeId(employeeId, status);
	}

	@GetMapping("/stats")
	@Operation(summary = "Статистика заданий работника (только админ)")
	public EmployeeTaskStats stats(@PathVariable Long employeeId) {
		return employeeTaskService.adminStats(employeeId);
	}

	@PostMapping
	@Operation(summary = "Выдать задание работнику (админ или себе)")
	@ApiResponse(responseCode = "201", description = "Задание создано")
	public ResponseEntity<EmployeeTaskResponse> create(
			@PathVariable Long employeeId,
			@Valid @RequestBody EmployeeTaskRequest request) {
		EmployeeTaskResponse created = employeeTaskService.create(employeeId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/tasks/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}
}
