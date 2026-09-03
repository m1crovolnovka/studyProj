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

import com.watchlist.app.domain.EpisodeStatus;
import com.watchlist.app.dto.EpisodeRequest;
import com.watchlist.app.dto.EpisodeResponse;
import com.watchlist.app.service.EpisodeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/titles")
@Tag(name = "Episodes", description = "Эпизоды сериалов")
public class EpisodeController {

	private final EpisodeService episodeService;

	public EpisodeController(EpisodeService episodeService) {
		this.episodeService = episodeService;
	}

	@GetMapping("/{titleId}/episodes")
	@Operation(summary = "Все эпизоды тайтла")
	public List<EpisodeResponse> findByTitleId(@PathVariable Long titleId) {
		return episodeService.findByTitleId(titleId);
	}

	@GetMapping("/{titleId}/episodes/season/{seasonNumber}")
	@Operation(summary = "Эпизоды по сезону")
	public List<EpisodeResponse> findBySeason(
			@PathVariable Long titleId,
			@PathVariable Integer seasonNumber) {
		return episodeService.findByTitleIdAndSeason(titleId, seasonNumber);
	}

	@GetMapping("/episodes/{id}")
	@Operation(summary = "Эпизод по id")
	public EpisodeResponse findById(@PathVariable Long id) {
		return episodeService.findById(id);
	}

	@PostMapping("/{titleId}/episodes")
	@Operation(summary = "Добавить эпизод")
	public ResponseEntity<EpisodeResponse> create(
			@PathVariable Long titleId,
			@Valid @RequestBody EpisodeRequest request) {
		EpisodeResponse created = episodeService.create(titleId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/episodes/{id}")
	@Operation(summary = "Полностью обновить эпизод")
	public EpisodeResponse update(
			@PathVariable Long id,
			@Valid @RequestBody EpisodeRequest request) {
		return episodeService.update(id, request);
	}

	@PutMapping("/episodes/{id}/status")
	@Operation(summary = "Изменить статус эпизода")
	public EpisodeResponse updateStatus(
			@PathVariable Long id,
			@RequestBody EpisodeStatus status) {
		return episodeService.updateStatus(id, status);
	}

	@DeleteMapping("/episodes/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Удалить эпизод")
	public void delete(@PathVariable Long id) {
		episodeService.delete(id);
	}
}
