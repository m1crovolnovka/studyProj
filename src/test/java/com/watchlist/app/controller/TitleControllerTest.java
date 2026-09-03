package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.watchlist.app.domain.TitleType;
import com.watchlist.app.domain.WatchStatus;
import com.watchlist.app.dto.TitleResponse;
import com.watchlist.app.dto.WatchlistStats;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.service.TitleService;

@WebMvcTest(controllers = TitleController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class TitleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TitleService titleService;

	@Test
	void getTitlesIsPublic() throws Exception {
		when(titleService.findAll(null, null)).thenReturn(List.of(
				new TitleResponse(1L, "Dune", TitleType.MOVIE, 2021, "Sci-Fi", WatchStatus.WATCHED, 9, null)));

		mockMvc.perform(get("/api/titles"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Dune"));
	}

	@Test
	void getStatsIsPublic() throws Exception {
		when(titleService.stats()).thenReturn(new WatchlistStats(3, 1, 1, 1));

		mockMvc.perform(get("/api/titles/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(3));
	}

	@Test
	void createRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/titles")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Dune","type":"MOVIE"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createWithAuth() throws Exception {
		when(titleService.create(any())).thenReturn(
				new TitleResponse(5L, "Dune", TitleType.MOVIE, 2021, "Sci-Fi", WatchStatus.TO_WATCH, null, null));

		mockMvc.perform(post("/api/titles")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Dune","type":"MOVIE","releaseYear":2021,"genre":"Sci-Fi"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/titles/5"))
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void createRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/titles")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"","type":"MOVIE"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void patchStatus() throws Exception {
		when(titleService.updateStatus(eq(1L), any())).thenReturn(
				new TitleResponse(1L, "Dune", TitleType.MOVIE, 2021, null, WatchStatus.WATCHING, null, null));

		mockMvc.perform(patch("/api/titles/1/status")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"watchStatus":"WATCHING"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.watchStatus").value("WATCHING"));
	}

	@Test
	void deleteMissingTitle() throws Exception {
		doThrow(new TitleNotFoundException(99L)).when(titleService).delete(99L);

		mockMvc.perform(delete("/api/titles/99").with(user("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteExistingTitle() throws Exception {
		doNothing().when(titleService).delete(1L);

		mockMvc.perform(delete("/api/titles/1").with(user("admin")))
				.andExpect(status().isNoContent());
	}
}
