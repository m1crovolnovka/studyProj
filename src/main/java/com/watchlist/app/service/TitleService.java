package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;
import com.watchlist.app.dto.StatusUpdateRequest;
import com.watchlist.app.dto.TitleRequest;
import com.watchlist.app.dto.TitleResponse;
import com.watchlist.app.dto.WatchlistStats;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.TitleRepository;

@Service
@Transactional
public class TitleService {

	private final TitleRepository titleRepository;

	public TitleService(TitleRepository titleRepository) {
		this.titleRepository = titleRepository;
	}

	@Transactional(readOnly = true)
	public List<TitleResponse> findAll(TitleType type, WatchStatus watchStatus) {
		List<Title> titles;
		if (type != null && watchStatus != null) {
			titles = titleRepository.findByTypeAndWatchStatus(type, watchStatus);
		}
		else if (type != null) {
			titles = titleRepository.findByType(type);
		}
		else if (watchStatus != null) {
			titles = titleRepository.findByWatchStatus(watchStatus);
		}
		else {
			titles = titleRepository.findAll();
		}
		return titles.stream().map(TitleResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public TitleResponse findById(Long id) {
		return TitleResponse.from(getTitle(id));
	}

	public TitleResponse create(TitleRequest request) {
		Title title = new Title();
		apply(title, request);
		if (title.getWatchStatus() == null) {
			title.setWatchStatus(WatchStatus.TO_WATCH);
		}
		return TitleResponse.from(titleRepository.save(title));
	}

	public TitleResponse update(Long id, TitleRequest request) {
		Title title = getTitle(id);
		apply(title, request);
		return TitleResponse.from(titleRepository.save(title));
	}

	public TitleResponse updateStatus(Long id, StatusUpdateRequest request) {
		Title title = getTitle(id);
		title.setWatchStatus(request.watchStatus());
		return TitleResponse.from(titleRepository.save(title));
	}

	public void delete(Long id) {
		if (!titleRepository.existsById(id)) {
			throw new TitleNotFoundException(id);
		}
		titleRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public WatchlistStats stats() {
		return new WatchlistStats(
				titleRepository.count(),
				titleRepository.countByWatchStatus(WatchStatus.TO_WATCH),
				titleRepository.countByWatchStatus(WatchStatus.WATCHING),
				titleRepository.countByWatchStatus(WatchStatus.WATCHED));
	}

	private Title getTitle(Long id) {
		return titleRepository.findById(id).orElseThrow(() -> new TitleNotFoundException(id));
	}

	private void apply(Title title, TitleRequest request) {
		title.setName(request.name());
		title.setType(request.type());
		title.setReleaseYear(request.releaseYear());
		title.setGenre(request.genre());
		if (request.watchStatus() != null) {
			title.setWatchStatus(request.watchStatus());
		}
		title.setRating(request.rating());
		title.setNotes(request.notes());
	}
}
