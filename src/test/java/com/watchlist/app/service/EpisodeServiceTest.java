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

import com.watchlist.app.domain.Episode;
import com.watchlist.app.domain.EpisodeStatus;
import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.dto.EpisodeRequest;
import com.watchlist.app.exception.EpisodeNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.EpisodeRepository;
import com.watchlist.app.repository.TitleRepository;

@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {

	@Mock
	private EpisodeRepository episodeRepository;

	@Mock
	private TitleRepository titleRepository;

	@InjectMocks
	private EpisodeService episodeService;

	@Test
	void createDefaultsStatusToWatch() {
		Title series = new Title();
		series.setId(1L);
		when(titleRepository.findById(1L)).thenReturn(Optional.of(series));
		when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> {
			Episode episode = invocation.getArgument(0);
			episode.setId(10L);
			return episode;
		});

		var created = episodeService.create(1L, new EpisodeRequest(1, 3, "Chapter 3", null, null, null));

		assertThat(created.id()).isEqualTo(10L);
		assertThat(created.episodeStatus()).isEqualTo(EpisodeStatus.TO_WATCH);
		assertThat(created.titleId()).isEqualTo(1L);
	}

	@Test
	void createThrowsWhenTitleMissing() {
		when(titleRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> episodeService.create(7L, new EpisodeRequest(1, 1, "Ep", null, null, null)))
				.isInstanceOf(TitleNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(episodeRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> episodeService.findById(7L))
				.isInstanceOf(EpisodeNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void updateStatusChangesEpisodeStatus() {
		Title series = new Title();
		series.setId(2L);
		Episode existing = new Episode();
		existing.setId(2L);
		existing.setTitle(series);
		existing.setEpisodeStatus(EpisodeStatus.TO_WATCH);
		when(episodeRepository.findById(2L)).thenReturn(Optional.of(existing));
		when(episodeRepository.save(existing)).thenReturn(existing);

		var updated = episodeService.updateStatus(2L, EpisodeStatus.WATCHED);

		assertThat(updated.episodeStatus()).isEqualTo(EpisodeStatus.WATCHED);
	}

	@Test
	void findByTitleIdThrowsWhenTitleMissing() {
		when(titleRepository.existsById(9L)).thenReturn(false);

		assertThatThrownBy(() -> episodeService.findByTitleId(9L))
				.isInstanceOf(TitleNotFoundException.class)
				.hasMessageContaining("9");
	}

	@Test
	void findByTitleIdReturnsEpisodes() {
		Title series = new Title();
		series.setId(1L);
		Episode ep = new Episode();
		ep.setId(5L);
		ep.setTitle(series);
		ep.setSeasonNumber(1);
		ep.setEpisodeNumber(2);
		when(titleRepository.existsById(1L)).thenReturn(true);
		when(episodeRepository.findByTitleId(1L)).thenReturn(List.of(ep));

		var result = episodeService.findByTitleId(1L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).seasonNumber()).isEqualTo(1);
		verify(episodeRepository).findByTitleId(1L);
	}
}
