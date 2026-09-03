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
import com.watchlist.app.dto.DepartmentResponse;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.DuplicateDepartmentException;
import com.watchlist.app.service.DepartmentService;

@WebMvcTest(controllers = DepartmentController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class DepartmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DepartmentService departmentService;

	@Test
	void getDepartmentsIsPublic() throws Exception {
		when(departmentService.findAll()).thenReturn(List.of(
				new DepartmentResponse(1L, "IT", "Office A")));

		mockMvc.perform(get("/api/departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("IT"));
	}

	@Test
	void createRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/departments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"IT"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createWithAuth() throws Exception {
		when(departmentService.create(any())).thenReturn(
				new DepartmentResponse(5L, "IT", "Office A"));

		mockMvc.perform(post("/api/departments")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"IT","location":"Office A"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/departments/5"))
				.andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void createRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/departments")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":""}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createDuplicateReturnsConflict() throws Exception {
		when(departmentService.create(any())).thenThrow(new DuplicateDepartmentException("IT"));

		mockMvc.perform(post("/api/departments")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"IT"}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void updateWithAuth() throws Exception {
		when(departmentService.update(eq(1L), any())).thenReturn(
				new DepartmentResponse(1L, "IT", "Office B"));

		mockMvc.perform(put("/api/departments/1")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"IT","location":"Office B"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.location").value("Office B"));
	}

	@Test
	void deleteMissingDepartment() throws Exception {
		doThrow(new DepartmentNotFoundException(99L)).when(departmentService).delete(99L);

		mockMvc.perform(delete("/api/departments/99").with(user("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteExistingDepartment() throws Exception {
		doNothing().when(departmentService).delete(1L);

		mockMvc.perform(delete("/api/departments/1").with(user("admin")))
				.andExpect(status().isNoContent());
	}
}
