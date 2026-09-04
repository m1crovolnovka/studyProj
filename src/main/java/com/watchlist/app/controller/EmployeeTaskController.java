package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.domain.TaskStatus;
import com.watchlist.app.dto.EmployeeTaskRequest;
import com.watchlist.app.dto.EmployeeTaskResponse;
import com.watchlist.app.dto.EmployeeTaskStats;
import com.watchlist.app.dto.EmployeeTaskStatusUpdateRequest;
import com.watchlist.app.service.EmployeeTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks", description = "Задания текущего пользователя")
public class EmployeeTaskController {

	private final EmployeeTaskService employeeTaskService;

	public EmployeeTaskController(EmployeeTaskService employeeTaskService) {
		this.employeeTaskService = employeeTaskService;
	}

	@GetMapping
	@Operation(summary = "Мои задания с опциональной фильтрацией по статусу")
	public List<EmployeeTaskResponse> findOwn(
			@RequestParam(required = false) TaskStatus status) {
		return employeeTaskService.findOwn(status);
	}

	@GetMapping("/stats")
	@Operation(summary = "Статистика моих заданий")
	public EmployeeTaskStats ownStats() {
		return employeeTaskService.ownStats();
	}

	@GetMapping("/{taskId}")
	@Operation(summary = "Задание по id (своё или любое для админа)")
	public EmployeeTaskResponse findById(@PathVariable Long taskId) {
		return employeeTaskService.findById(taskId);
	}

	@PostMapping
	@Operation(summary = "Выдать задание себе")
	@ApiResponse(responseCode = "201", description = "Задание создано")
	public ResponseEntity<EmployeeTaskResponse> createOwn(
			@Valid @RequestBody EmployeeTaskRequest request) {
		EmployeeTaskResponse created = employeeTaskService.createOwn(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PostMapping("/{taskId}/complete")
	@Operation(summary = "Выполнить задание")
	public EmployeeTaskResponse complete(@PathVariable Long taskId) {
		return employeeTaskService.complete(taskId);
	}

	@PatchMapping("/{taskId}/status")
	@Operation(summary = "Изменить статус задания")
	public EmployeeTaskResponse updateStatus(
			@PathVariable Long taskId,
			@Valid @RequestBody EmployeeTaskStatusUpdateRequest request) {
		return employeeTaskService.updateStatus(taskId, request);
	}

	@DeleteMapping("/{taskId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Удалить задание (админ или создатель)")
	public void delete(@PathVariable Long taskId) {
		employeeTaskService.delete(taskId);
	}
}
