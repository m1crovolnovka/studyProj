package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.dto.EmployeeRequest;
import com.watchlist.app.dto.EmployeeResponse;
import com.watchlist.app.dto.SalarySyncResponse;
import com.watchlist.app.service.EmployeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Employees", description = "Работники подразделений")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	@GetMapping("/departments/{departmentId}/employees")
	@Operation(summary = "Все работники подразделения")
	public List<EmployeeResponse> findByDepartmentId(
			@PathVariable Long departmentId,
			@RequestParam(required = false) Long positionId) {
		if (positionId != null) {
			return employeeService.findByDepartmentIdAndPosition(departmentId, positionId);
		}
		return employeeService.findByDepartmentId(departmentId);
	}

	@GetMapping("/employees/{id}")
	@Operation(summary = "Работник по id")
	public EmployeeResponse findById(@PathVariable Long id) {
		return employeeService.findById(id);
	}

	@PostMapping("/departments/{departmentId}/employees")
	@Operation(summary = "Добавить работника в подразделение")
	@ApiResponse(responseCode = "201", description = "Работник создан")
	public ResponseEntity<EmployeeResponse> create(
			@PathVariable Long departmentId,
			@Valid @RequestBody EmployeeRequest request) {
		EmployeeResponse created = employeeService.create(departmentId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/employees/{id}")
	@Operation(summary = "Полностью обновить работника")
	public EmployeeResponse update(
			@PathVariable Long id,
			@Valid @RequestBody EmployeeRequest request) {
		return employeeService.update(id, request);
	}

	@PostMapping("/employees/sync-salaries")
	@Operation(summary = "Синхронизировать зарплаты работников по коэффициентам должностей")
	public SalarySyncResponse synchronizeSalaries() {
		return employeeService.synchronizeSalaries();
	}

	@DeleteMapping("/employees/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Удалить работника")
	public void delete(@PathVariable Long id) {
		employeeService.delete(id);
	}
}
