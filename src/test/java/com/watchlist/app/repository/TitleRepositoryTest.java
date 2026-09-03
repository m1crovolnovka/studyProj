package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;

@DataJpaTest
@ActiveProfiles("test")
class TitleRepositoryTest {

	@Autowired
	private TitleRepository titleRepository;

	@Test
	void savesAndFiltersByTypeAndStatus() {
		titleRepository.save(title("Dune", TitleType.MOVIE, WatchStatus.WATCHED));
		titleRepository.save(title("Andor", TitleType.SERIES, WatchStatus.WATCHING));
		titleRepository.save(title("Arrival", TitleType.MOVIE, WatchStatus.TO_WATCH));

		assertThat(titleRepository.findByType(TitleType.MOVIE)).hasSize(2);
		assertThat(titleRepository.findByWatchStatus(WatchStatus.WATCHING))
				.extracting(Title::getName)
				.containsExactly("Andor");
		assertThat(titleRepository.findByTypeAndWatchStatus(TitleType.MOVIE, WatchStatus.WATCHED))
				.extracting(Title::getName)
				.containsExactly("Dune");
		assertThat(titleRepository.countByWatchStatus(WatchStatus.TO_WATCH)).isEqualTo(1);
	}

	private Title title(String name, TitleType type, WatchStatus status) {
		Title title = new Title();
		title.setName(name);
		title.setType(type);
		title.setWatchStatus(status);
		return title;
	}
}
