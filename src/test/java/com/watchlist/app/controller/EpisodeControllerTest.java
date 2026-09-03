package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.watchlist.app.config.SecurityConfig;
import com.watchlist.app.domain.EpisodeStatus;
import com.watchlist.app.dto.EpisodeResponse;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.EpisodeNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.service.EpisodeService;

@WebMvcTest(controllers = EpisodeController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class EpisodeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EpisodeService episodeService;

	@Test
	void getEpisodesIsPublic() throws Exception {
		when(episodeService.findByTitleId(1L)).thenReturn(List.of(
				new EpisodeResponse(3L, 1L, 1, 2, "Chapter 2", EpisodeStatus.TO_WATCH, null, null)));

		mockMvc.perform(get("/api/titles/1/episodes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Chapter 2"));
	}

	@Test
	void getEpisodesBySeason() throws Exception {
		when(episodeService.findByTitleIdAndSeason(1L, 2)).thenReturn(List.of(
				new EpisodeResponse(4L, 1L, 2, 1, "Finale", EpisodeStatus.WATCHED, 9, null)));

		mockMvc.perform(get("/api/titles/1/episodes/season/2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].episodeNumber").value(1))
				.andExpect(jsonPath("$[0].episodeStatus").value("WATCHED"));
	}

	@Test
	void createRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/titles/1/episodes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"seasonNumber":1,"episodeNumber":1,"name":"Pilot"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createWithAuth() throws Exception {
		when(episodeService.create(eq(1L), any())).thenReturn(
				new EpisodeResponse(5L, 1L, 1, 1, "Pilot", EpisodeStatus.TO_WATCH, null, null));

		mockMvc.perform(post("/api/titles/1/episodes")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"seasonNumber":1,"episodeNumber":1,"name":"Pilot"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/titles/1/episodes/5"))
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void createRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/titles/1/episodes")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"seasonNumber":0,"episodeNumber":1,"name":""}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateStatus() throws Exception {
		when(episodeService.updateStatus(1L, EpisodeStatus.WATCHED)).thenReturn(
				new EpisodeResponse(1L, 1L, 1, 1, "Pilot", EpisodeStatus.WATCHED, 8, null));

		mockMvc.perform(put("/api/titles/episodes/1/status")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"WATCHED\""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.episodeStatus").value("WATCHED"));
	}

	@Test
	void createThrowsWhenTitleMissing() throws Exception {
		doThrow(new TitleNotFoundException(99L)).when(episodeService).create(eq(99L), any());

		mockMvc.perform(post("/api/titles/99/episodes")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"seasonNumber":1,"episodeNumber":1,"name":"Pilot"}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteMissingEpisode() throws Exception {
		doThrow(new EpisodeNotFoundException(99L)).when(episodeService).delete(99L);

		mockMvc.perform(delete("/api/titles/episodes/99").with(user("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteExistingEpisode() throws Exception {
		doNothing().when(episodeService).delete(1L);

		mockMvc.perform(delete("/api/titles/episodes/1").with(user("admin")))
				.andExpect(status().isNoContent());
	}
}
