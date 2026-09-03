package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.watchlist.app.domain.Position;
import com.watchlist.app.dto.PositionRequest;
import com.watchlist.app.exception.DuplicatePositionException;
import com.watchlist.app.exception.PositionNotFoundException;
import com.watchlist.app.repository.PositionRepository;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

	@Mock
	private PositionRepository positionRepository;

	@InjectMocks
	private PositionService positionService;

	@Test
	void createSavesAndReturnsPosition() {
		when(positionRepository.existsByName("SENIOR")).thenReturn(false);
		when(positionRepository.save(any(Position.class))).thenAnswer(invocation -> {
			Position position = invocation.getArgument(0);
			position.setId(1L);
			return position;
		});

		var created = positionService.create(new PositionRequest("SENIOR", new BigDecimal("1.50")));

		assertThat(created.id()).isEqualTo(1L);
		assertThat(created.name()).isEqualTo("SENIOR");
		assertThat(created.coefficient()).isEqualByComparingTo("1.50");
	}

	@Test
	void createThrowsWhenPositionAlreadyExists() {
		when(positionRepository.existsByName("SENIOR")).thenReturn(true);

		assertThatThrownBy(() -> positionService.create(new PositionRequest("SENIOR", new BigDecimal("1.50"))))
				.isInstanceOf(DuplicatePositionException.class)
				.hasMessageContaining("SENIOR");
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(positionRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> positionService.findById(7L))
				.isInstanceOf(PositionNotFoundException.class)
				.hasMessageContaining("7");
	}
}
