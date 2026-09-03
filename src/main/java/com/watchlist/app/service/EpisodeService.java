package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Episode;
import com.watchlist.app.domain.EpisodeStatus;
import com.watchlist.app.domain.Title;
import com.watchlist.app.dto.EpisodeRequest;
import com.watchlist.app.dto.EpisodeResponse;
import com.watchlist.app.exception.EpisodeNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.repository.EpisodeRepository;
import com.watchlist.app.repository.TitleRepository;

@Service
@Transactional
public class EpisodeService {

	private final EpisodeRepository episodeRepository;
	private final TitleRepository titleRepository;

	public EpisodeService(EpisodeRepository episodeRepository, TitleRepository titleRepository) {
		this.episodeRepository = episodeRepository;
		this.titleRepository = titleRepository;
	}

	@Transactional(readOnly = true)
	public List<EpisodeResponse> findByTitleId(Long titleId) {
		checkTitleExists(titleId);
		return episodeRepository.findByTitleId(titleId).stream()
				.map(EpisodeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EpisodeResponse> findByTitleIdAndSeason(Long titleId, Integer seasonNumber) {
		checkTitleExists(titleId);
		return episodeRepository.findByTitleIdAndSeasonNumber(titleId, seasonNumber).stream()
				.map(EpisodeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public EpisodeResponse findById(Long id) {
		return EpisodeResponse.from(getEpisode(id));
	}

	public EpisodeResponse create(Long titleId, EpisodeRequest request) {
		Title title = titleRepository.findById(titleId)
				.orElseThrow(() -> new TitleNotFoundException(titleId));
		Episode episode = new Episode();
		episode.setTitle(title);
		apply(episode, request);
		if (episode.getEpisodeStatus() == null) {
			episode.setEpisodeStatus(EpisodeStatus.TO_WATCH);
		}
		return EpisodeResponse.from(episodeRepository.save(episode));
	}

	public EpisodeResponse update(Long id, EpisodeRequest request) {
		Episode episode = getEpisode(id);
		apply(episode, request);
		return EpisodeResponse.from(episodeRepository.save(episode));
	}

	public EpisodeResponse updateStatus(Long id, EpisodeStatus status) {
		Episode episode = getEpisode(id);
		episode.setEpisodeStatus(status);
		return EpisodeResponse.from(episodeRepository.save(episode));
	}

	public void delete(Long id) {
		if (!episodeRepository.existsById(id)) {
			throw new EpisodeNotFoundException(id);
		}
		episodeRepository.deleteById(id);
	}

	private Episode getEpisode(Long id) {
		return episodeRepository.findById(id)
				.orElseThrow(() -> new EpisodeNotFoundException(id));
	}

	private void checkTitleExists(Long titleId) {
		if (!titleRepository.existsById(titleId)) {
			throw new TitleNotFoundException(titleId);
		}
	}

	private void apply(Episode episode, EpisodeRequest request) {
		episode.setSeasonNumber(request.seasonNumber());
		episode.setEpisodeNumber(request.episodeNumber());
		episode.setName(request.name());
		if (request.episodeStatus() != null) {
			episode.setEpisodeStatus(request.episodeStatus());
		}
		episode.setRating(request.rating());
		episode.setNotes(request.notes());
	}
}
