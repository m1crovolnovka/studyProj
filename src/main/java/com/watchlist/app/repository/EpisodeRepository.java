package com.watchlist.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Episode;
import com.watchlist.app.domain.EpisodeStatus;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

	List<Episode> findByTitleId(Long titleId);

	List<Episode> findByTitleIdAndSeasonNumber(Long titleId, Integer seasonNumber);

	List<Episode> findByTitleIdAndEpisodeStatus(Long titleId, EpisodeStatus episodeStatus);

	long countByTitleIdAndEpisodeStatus(Long titleId, EpisodeStatus episodeStatus);
}
