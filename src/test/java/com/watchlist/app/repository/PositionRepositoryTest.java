package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Position;

@DataJpaTest
@ActiveProfiles("test")
class PositionRepositoryTest {

	@Autowired
	private PositionRepository positionRepository;

	@Test
	void savesAndFindsPositionByName() {
		Position position = new Position();
		position.setName("SENIOR");
		position.setCoefficient(new BigDecimal("1.50"));

		positionRepository.save(position);

		Position found = positionRepository.findByName("SENIOR").orElseThrow();
		assertThat(found.getCoefficient()).isEqualByComparingTo("1.50");
	}
}
