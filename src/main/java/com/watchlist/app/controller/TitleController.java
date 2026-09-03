package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;
import com.watchlist.app.dto.StatusUpdateRequest;
import com.watchlist.app.dto.TitleRequest;
import com.watchlist.app.dto.TitleResponse;
import com.watchlist.app.dto.WatchlistStats;
import com.watchlist.app.service.TitleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/titles")
@Tag(name = "Titles", description = "Список фильмов и сериалов к просмотру")
public class TitleController {

	private final TitleService titleService;

	public TitleController(TitleService titleService) {
		this.titleService = titleService;
	}

	@GetMapping
	@Operation(summary = "Список тайтлов с опциональной фильтрацией")
	public List<TitleResponse> findAll(
			@RequestParam(required = false) TitleType type,
			@RequestParam(required = false) WatchStatus watchStatus) {
		return titleService.findAll(type, watchStatus);
	}

	@GetMapping("/stats")
	@Operation(summary = "Статистика списка")
	public WatchlistStats stats() {
		return titleService.stats();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Тайтл по id")
	public TitleResponse findById(@PathVariable Long id) {
		return titleService.findById(id);
	}

	@PostMapping
	@Operation(summary = "Добавить фильм или сериал")
	@ApiResponse(responseCode = "201",description = "Тайт отлично создан")
	public ResponseEntity<TitleResponse> create(@Valid @RequestBody TitleRequest request) {
		TitleResponse created = titleService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Полностью обновить тайтл")
	public TitleResponse update(@PathVariable Long id, @Valid @RequestBody TitleRequest request) {
		return titleService.update(id, request);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Изменить статус просмотра")
	public TitleResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
		return titleService.updateStatus(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Удалить тайтл")
	public void delete(@PathVariable Long id) {
		titleService.delete(id);
	}
}
