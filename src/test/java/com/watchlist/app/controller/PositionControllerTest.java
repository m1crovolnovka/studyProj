package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.watchlist.app.config.TestSecurityConfig;
import com.watchlist.app.dto.PositionResponse;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.service.PositionService;

@WebMvcTest(controllers = PositionController.class)
@AutoConfigureMockMvc
@Import({ TestSecurityConfig.class, ApiExceptionHandler.class })
class PositionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PositionService positionService;

	@Test
	void getPositionsIsPublic() throws Exception {
		when(positionService.findAll()).thenReturn(List.of(new PositionResponse(1L, "SENIOR", new BigDecimal("1.50"))));

		mockMvc.perform(get("/api/positions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("SENIOR"));
	}

	@Test
	void createPositionRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/positions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"SENIOR","coefficient":1.5}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createPositionWithAuth() throws Exception {
		when(positionService.create(any())).thenReturn(new PositionResponse(1L, "SENIOR", new BigDecimal("1.50")));

		mockMvc.perform(post("/api/positions")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"SENIOR","coefficient":1.5}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/positions/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void updatePositionWithAuth() throws Exception {
		when(positionService.update(eq(1L), any())).thenReturn(new PositionResponse(1L, "LEAD", new BigDecimal("1.80")));

		mockMvc.perform(put("/api/positions/1")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"LEAD","coefficient":1.8}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("LEAD"));
	}

	@Test
	void createPositionForbiddenForRegularUser() throws Exception {
		mockMvc.perform(post("/api/positions")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"SENIOR","coefficient":1.5}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatePositionForbiddenForRegularUser() throws Exception {
		mockMvc.perform(put("/api/positions/1")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"LEAD","coefficient":1.8}
						"""))
				.andExpect(status().isForbidden());
	}
}
