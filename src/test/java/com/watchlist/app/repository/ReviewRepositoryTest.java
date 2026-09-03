package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Review;
import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;

@DataJpaTest
@ActiveProfiles("test")
class ReviewRepositoryTest {

	@Autowired
	private ReviewRepository reviewRepository;

	@Autowired
	private TitleRepository titleRepository;

	@Test
	void savesAndListsReviewsByTitle() {
		Title movie = titleRepository.save(title("Dune", TitleType.MOVIE));
		Title other = titleRepository.save(title("Blade Runner", TitleType.MOVIE));

		reviewRepository.save(review(movie, "Great world-building"));
		reviewRepository.save(review(movie, "Too long"));

		assertThat(reviewRepository.findByTitleIdOrderByCreatedAtDesc(movie.getId())).hasSize(2);
		assertThat(reviewRepository.countByTitleId(movie.getId())).isEqualTo(2);
		assertThat(reviewRepository.countByTitleId(other.getId())).isZero();
	}

	private Title title(String name, TitleType type) {
		Title title = new Title();
		title.setName(name);
		title.setType(type);
		return title;
	}

	private Review review(Title title, String content) {
		Review review = new Review();
		review.setTitle(title);
		review.setContent(content);
		return review;
	}
}
