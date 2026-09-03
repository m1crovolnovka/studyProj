package com.watchlist.app.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.watchlist.app.dto.ReviewRequest;
import com.watchlist.app.dto.ReviewResponse;
import com.watchlist.app.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/titles")
@Tag(name = "Reviews", description = "Отзывы и заметки к тайтлам")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@GetMapping("/{titleId}/reviews")
	@Operation(summary = "Все отзывы тайтла")
	public List<ReviewResponse> findByTitleId(@PathVariable Long titleId) {
		return reviewService.findByTitleId(titleId);
	}

	@GetMapping("/reviews/{id}")
	@Operation(summary = "Отзыв по id")
	public ReviewResponse findById(@PathVariable Long id) {
		return reviewService.findById(id);
	}

	@PostMapping("/{titleId}/reviews")
	@Operation(summary = "Добавить отзыв")
	@ApiResponse(responseCode = "201",description = "Тайт отлично создан")
	public ResponseEntity<ReviewResponse> create(
			@PathVariable Long titleId,
			@Valid @RequestBody ReviewRequest request) {
		ReviewResponse created = reviewService.create(titleId, request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/reviews/{id}")
	@Operation(summary = "Обновить отзыв")
	public ReviewResponse update(
			@PathVariable Long id,
			@Valid @RequestBody ReviewRequest request) {
		return reviewService.update(id, request);
	}

	@DeleteMapping("/reviews/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Удалить отзыв")
	public void delete(@PathVariable Long id) {
		reviewService.delete(id);
	}
}
