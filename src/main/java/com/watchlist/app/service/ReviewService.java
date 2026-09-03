package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Review;
import com.watchlist.app.domain.Title;
import com.watchlist.app.dto.ReviewRequest;
import com.watchlist.app.dto.ReviewResponse;
import com.watchlist.app.exception.ReviewNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.ReviewRepository;
import com.watchlist.app.repository.TitleRepository;

@Service
@Transactional
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final TitleRepository titleRepository;

	public ReviewService(ReviewRepository reviewRepository, TitleRepository titleRepository) {
		this.reviewRepository = reviewRepository;
		this.titleRepository = titleRepository;
	}

	@Transactional(readOnly = true)
	public List<ReviewResponse> findByTitleId(Long titleId) {
		checkTitleExists(titleId);
		return reviewRepository.findByTitleIdOrderByCreatedAtDesc(titleId).stream()
				.map(ReviewResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public ReviewResponse findById(Long id) {
		return ReviewResponse.from(getReview(id));
	}

	public ReviewResponse create(Long titleId, ReviewRequest request) {
		Title title = titleRepository.findById(titleId)
				.orElseThrow(() -> new TitleNotFoundException(titleId));
		Review review = new Review();
		review.setTitle(title);
		review.setContent(request.content());
		review.setRating(request.rating());
		return ReviewResponse.from(reviewRepository.save(review));
	}

	public ReviewResponse update(Long id, ReviewRequest request) {
		Review review = getReview(id);
		review.setContent(request.content());
		review.setRating(request.rating());
		return ReviewResponse.from(reviewRepository.save(review));
	}

	public void delete(Long id) {
		if (!reviewRepository.existsById(id)) {
			throw new ReviewNotFoundException(id);
		}
		reviewRepository.deleteById(id);
	}

	private Review getReview(Long id) {
		return reviewRepository.findById(id)
				.orElseThrow(() -> new ReviewNotFoundException(id));
	}

	private void checkTitleExists(Long titleId) {
		if (!titleRepository.existsById(titleId)) {
			throw new TitleNotFoundException(titleId);
		}
	}
}
