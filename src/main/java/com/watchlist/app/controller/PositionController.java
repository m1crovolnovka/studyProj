package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.dto.PositionRequest;
import com.watchlist.app.dto.PositionResponse;
import com.watchlist.app.service.PositionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/positions")
@Tag(name = "Positions", description = "Должности сотрудников")
public class PositionController {

	private final PositionService positionService;

	public PositionController(PositionService positionService) {
		this.positionService = positionService;
	}

	@GetMapping
	@Operation(summary = "Все должности")
	public List<PositionResponse> findAll() {
		return positionService.findAll();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Должность по id")
	public PositionResponse findById(@PathVariable Long id) {
		return positionService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Создать должность")
	@ApiResponse(responseCode = "201", description = "Должность создана")
	public ResponseEntity<PositionResponse> create(@Valid @RequestBody PositionRequest request) {
		PositionResponse created = positionService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Полностью обновить должность")
	public PositionResponse update(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
		return positionService.update(id, request);
	}
}
