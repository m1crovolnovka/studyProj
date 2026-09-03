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

import java.time.LocalDateTime;
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
import com.watchlist.app.dto.ReviewResponse;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.ReviewNotFoundException;
import com.watchlist.app.exception.TitleNotFoundException;
import com.watchlist.app.service.ReviewService;

@WebMvcTest(controllers = ReviewController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@Test
	void getReviewsIsPublic() throws Exception {
		when(reviewService.findByTitleId(1L)).thenReturn(List.of(
				new ReviewResponse(3L, 1L, "Loved it", 9, LocalDateTime.of(2026, 1, 1, 12, 0), null)));

		mockMvc.perform(get("/api/titles/1/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].content").value("Loved it"))
				.andExpect(jsonPath("$[0].rating").value(9));
	}

	@Test
	void createRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/titles/1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content":"Loved it","rating":9}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createWithAuth() throws Exception {
		when(reviewService.create(eq(1L), any())).thenReturn(
				new ReviewResponse(5L, 1L, "Loved it", 9, LocalDateTime.now(), null));

		mockMvc.perform(post("/api/titles/1/reviews")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content":"Loved it","rating":9}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/titles/1/reviews/5"))
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void createRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/titles/1/reviews")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content":"","rating":99}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateWithAuth() throws Exception {
		when(reviewService.update(eq(2L), any())).thenReturn(
				new ReviewResponse(2L, 1L, "Changed", 7, LocalDateTime.now(), LocalDateTime.now()));

		mockMvc.perform(put("/api/titles/reviews/2")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content":"Changed","rating":7}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Changed"))
				.andExpect(jsonPath("$.rating").value(7));
	}

	@Test
	void createThrowsWhenTitleMissing() throws Exception {
		doThrow(new TitleNotFoundException(99L)).when(reviewService).create(eq(99L), any());

		mockMvc.perform(post("/api/titles/99/reviews")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"content":"Loved it","rating":9}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteMissingReview() throws Exception {
		doThrow(new ReviewNotFoundException(99L)).when(reviewService).delete(99L);

		mockMvc.perform(delete("/api/titles/reviews/99").with(user("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteExistingReview() throws Exception {
		doNothing().when(reviewService).delete(1L);

		mockMvc.perform(delete("/api/titles/reviews/1").with(user("admin")))
				.andExpect(status().isNoContent());
	}
}
