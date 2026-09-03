package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.watchlist.app.domain.Review;
import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.dto.ReviewRequest;
import com.watchlist.app.exception.ReviewNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.ReviewRepository;
import com.watchlist.app.repository.TitleRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private TitleRepository titleRepository;

	@InjectMocks
	private ReviewService reviewService;

	@Test
	void createSetsTitleAndReturnsResponse() {
		Title movie = new Title();
		movie.setId(1L);
		when(titleRepository.findById(1L)).thenReturn(Optional.of(movie));
		when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
			Review review = invocation.getArgument(0);
			review.setId(11L);
			return review;
		});

		var created = reviewService.create(1L, new ReviewRequest("Loved it", 9));

		assertThat(created.id()).isEqualTo(11L);
		assertThat(created.titleId()).isEqualTo(1L);
		assertThat(created.content()).isEqualTo("Loved it");
		assertThat(created.rating()).isEqualTo(9);
	}

	@Test
	void createThrowsWhenTitleMissing() {
		when(titleRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.create(7L, new ReviewRequest("text", 5)))
				.isInstanceOf(TitleNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(reviewRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> reviewService.findById(7L))
				.isInstanceOf(ReviewNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void updateChangesContentAndRating() {
		Title movie = new Title();
		movie.setId(1L);
		Review existing = new Review();
		existing.setId(2L);
		existing.setTitle(movie);
		existing.setContent("Old");
		existing.setRating(3);
		when(reviewRepository.findById(2L)).thenReturn(Optional.of(existing));
		when(reviewRepository.save(existing)).thenReturn(existing);

		var updated = reviewService.update(2L, new ReviewRequest("New", 10));

		assertThat(updated.content()).isEqualTo("New");
		assertThat(updated.rating()).isEqualTo(10);
	}

	@Test
	void findByTitleIdThrowsWhenTitleMissing() {
		when(titleRepository.existsById(9L)).thenReturn(false);

		assertThatThrownBy(() -> reviewService.findByTitleId(9L))
				.isInstanceOf(TitleNotFoundException.class)
				.hasMessageContaining("9");
	}

	@Test
	void findByTitleIdReturnsReviews() {
		Title movie = new Title();
		movie.setId(1L);
		Review review = new Review();
		review.setId(5L);
		review.setTitle(movie);
		review.setContent("Nice");
		when(titleRepository.existsById(1L)).thenReturn(true);
		when(reviewRepository.findByTitleIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(review));

		var result = reviewService.findByTitleId(1L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).content()).isEqualTo("Nice");
		verify(reviewRepository).findByTitleIdOrderByCreatedAtDesc(1L);
	}
}
