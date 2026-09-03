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

import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;
import com.watchlist.app.dto.StatusUpdateRequest;
import com.watchlist.app.dto.TitleRequest;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.TitleRepository;

@ExtendWith(MockitoExtension.class)
class TitleServiceTest {

	@Mock
	private TitleRepository titleRepository;

	@InjectMocks
	private TitleService titleService;

	@Test
	void createDefaultsStatusToWatch() {
		when(titleRepository.save(any(Title.class))).thenAnswer(invocation -> {
			Title title = invocation.getArgument(0);
			title.setId(1L);
			return title;
		});

		var created = titleService.create(new TitleRequest("Dune", TitleType.MOVIE, 2021, "Sci-Fi", null, 9, null));

		assertThat(created.id()).isEqualTo(1L);
		assertThat(created.watchStatus()).isEqualTo(WatchStatus.TO_WATCH);
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(titleRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> titleService.findById(7L))
				.isInstanceOf(TitleNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void updateStatusChangesWatchStatus() {
		Title existing = new Title();
		existing.setId(2L);
		existing.setName("Andor");
		existing.setType(TitleType.SERIES);
		existing.setWatchStatus(WatchStatus.TO_WATCH);
		when(titleRepository.findById(2L)).thenReturn(Optional.of(existing));
		when(titleRepository.save(existing)).thenReturn(existing);

		var updated = titleService.updateStatus(2L, new StatusUpdateRequest(WatchStatus.WATCHING));

		assertThat(updated.watchStatus()).isEqualTo(WatchStatus.WATCHING);
	}

	@Test
	void findAllUsesCombinedFilter() {
		when(titleRepository.findByTypeAndWatchStatus(TitleType.MOVIE, WatchStatus.WATCHED)).thenReturn(List.of());

		assertThat(titleService.findAll(TitleType.MOVIE, WatchStatus.WATCHED)).isEmpty();
		verify(titleRepository).findByTypeAndWatchStatus(TitleType.MOVIE, WatchStatus.WATCHED);
	}
}
