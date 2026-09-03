package com.watchlist.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Title;
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;

public interface TitleRepository extends JpaRepository<Title, Long> {

	List<Title> findByType(TitleType type);

	List<Title> findByWatchStatus(WatchStatus watchStatus);

	List<Title> findByTypeAndWatchStatus(TitleType type, WatchStatus watchStatus);

	long countByWatchStatus(WatchStatus watchStatus);
}
