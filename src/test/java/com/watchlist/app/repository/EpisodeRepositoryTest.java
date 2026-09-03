package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Episode;
import com.watchlist.app.domain.EpisodeStatus;
import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;

@DataJpaTest
@ActiveProfiles("test")
class EpisodeRepositoryTest {

	@Autowired
	private EpisodeRepository episodeRepository;

	@Autowired
	private TitleRepository titleRepository;

	@Test
	void savesAndFiltersEpisodesByTitleAndSeason() {
		Title series = titleRepository.save(title("Andor", TitleType.SERIES));
		Title other = titleRepository.save(title("The Bear", TitleType.SERIES));

		episodeRepository.save(episode(series, 1, 1, EpisodeStatus.WATCHED));
		episodeRepository.save(episode(series, 1, 2, EpisodeStatus.TO_WATCH));
		episodeRepository.save(episode(series, 2, 1, EpisodeStatus.TO_WATCH));
		episodeRepository.save(episode(other, 1, 1, EpisodeStatus.WATCHED));

		assertThat(episodeRepository.findByTitleId(series.getId())).hasSize(3);
		assertThat(episodeRepository.findByTitleIdAndSeasonNumber(series.getId(), 1)).hasSize(2);
		assertThat(episodeRepository.findByTitleIdAndEpisodeStatus(series.getId(), EpisodeStatus.TO_WATCH)).hasSize(2);
		assertThat(episodeRepository.countByTitleIdAndEpisodeStatus(series.getId(), EpisodeStatus.WATCHED)).isEqualTo(1);
	}

	private Title title(String name, TitleType type) {
		Title title = new Title();
		title.setName(name);
		title.setType(type);
		return title;
	}

	private Episode episode(Title series, int season, int number, EpisodeStatus status) {
		Episode episode = new Episode();
		episode.setTitle(series);
		episode.setSeasonNumber(season);
		episode.setEpisodeNumber(number);
		episode.setEpisodeStatus(status);
		return episode;
	}
}
