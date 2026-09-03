package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Position;
import com.watchlist.app.dto.PositionRequest;
import com.watchlist.app.dto.PositionResponse;
import com.watchlist.app.exception.DuplicatePositionException;
import com.watchlist.app.exception.PositionNotFoundException;
import com.watchlist.app.repository.PositionRepository;

@Service
@Transactional
public class PositionService {

	private final PositionRepository positionRepository;

	public PositionService(PositionRepository positionRepository) {
		this.positionRepository = positionRepository;
	}

	@Transactional(readOnly = true)
	public List<PositionResponse> findAll() {
		return positionRepository.findAll().stream()
				.map(PositionResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public PositionResponse findById(Long id) {
		return PositionResponse.from(getPosition(id));
	}

	public PositionResponse create(PositionRequest request) {
		if (positionRepository.existsByName(request.name())) {
			throw new DuplicatePositionException(request.name());
		}

		Position position = new Position();
		apply(position, request);
		return PositionResponse.from(positionRepository.save(position));
	}

	public PositionResponse update(Long id, PositionRequest request) {
		Position position = getPosition(id);
		positionRepository.findByName(request.name())
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new DuplicatePositionException(request.name());
				});
		apply(position, request);
		return PositionResponse.from(positionRepository.save(position));
	}

	private Position getPosition(Long id) {
		return positionRepository.findById(id)
				.orElseThrow(() -> new PositionNotFoundException(id));
	}

	private void apply(Position position, PositionRequest request) {
		position.setName(request.name());
		position.setCoefficient(request.coefficient());
	}
}
