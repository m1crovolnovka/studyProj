package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.watchlist.app.config.TestSecurityConfig;
import com.watchlist.app.domain.TaskStatus;
import com.watchlist.app.dto.EmployeeTaskResponse;
import com.watchlist.app.dto.EmployeeTaskStats;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.service.EmployeeTaskService;

@WebMvcTest(controllers = EmployeeTaskController.class)
@AutoConfigureMockMvc
@Import({ TestSecurityConfig.class, ApiExceptionHandler.class })
class EmployeeTaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeTaskService employeeTaskService;

	private final EmployeeTaskResponse taskResponse = new EmployeeTaskResponse(
			10L, 5L, "Write report", "Quarterly", TaskStatus.PENDING,
			Instant.parse("2026-09-04T08:00:00Z"), null);

	@Test
	void getOwnTasksRequiresAuth() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getOwnTasks() throws Exception {
		when(employeeTaskService.findOwn(null)).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/tasks").with(user("bob").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Write report"));
	}

	@Test
	void getOwnStats() throws Exception {
		when(employeeTaskService.ownStats()).thenReturn(new EmployeeTaskStats(3, 1, 1, 1));

		mockMvc.perform(get("/api/tasks/stats").with(user("bob").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(3));
	}

	@Test
	void getTaskById() throws Exception {
		when(employeeTaskService.findById(10L)).thenReturn(taskResponse);

		mockMvc.perform(get("/api/tasks/10").with(user("bob").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10));
	}

	@Test
	void createOwnTask() throws Exception {
		EmployeeTaskResponse created = new EmployeeTaskResponse(
				1L, 5L, "Fix bug", "Fix the login bug", TaskStatus.PENDING,
				Instant.parse("2026-09-04T08:00:00Z"), null);
		when(employeeTaskService.createOwn(any())).thenReturn(created);

		mockMvc.perform(post("/api/tasks")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Fix bug","description":"Fix the login bug"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/tasks/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void createOwnTaskRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/tasks")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Fix bug"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void completeTask() throws Exception {
		EmployeeTaskResponse completed = new EmployeeTaskResponse(
				1L, 5L, "Fix bug", "Quarterly", TaskStatus.COMPLETED,
				Instant.parse("2026-07-04T08:00:00Z"), Instant.parse("2026-07-04T09:00:00Z"));
		when(employeeTaskService.complete(1L)).thenReturn(completed);

		mockMvc.perform(post("/api/tasks/1/complete").with(user("bob").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void completeTaskForbiddenForNonAssignee() throws Exception {
		doThrow(new AccessDeniedException("Access denied"))
				.when(employeeTaskService).complete(1L);

		mockMvc.perform(post("/api/tasks/1/complete").with(user("bob").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void patchTaskStatus() throws Exception {
		EmployeeTaskResponse completed = new EmployeeTaskResponse(
				1L, 5L, "Fix bug", "Quarterly", TaskStatus.COMPLETED,
				Instant.parse("2026-07-04T08:00:00Z"), Instant.parse("2026-07-04T09:00:00Z"));
		when(employeeTaskService.updateStatus(eq(1L), any())).thenReturn(completed);

		mockMvc.perform(patch("/api/tasks/1/status")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"status":"COMPLETED"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	@Test
	void deleteOwnCreatedTask() throws Exception {
		mockMvc.perform(delete("/api/tasks/1").with(user("bob").roles("USER")))
				.andExpect(status().isNoContent());
		verify(employeeTaskService).delete(1L);
	}

	@Test
	void deleteForbiddenForNonCreator() throws Exception {
		doThrow(new AccessDeniedException("Access denied"))
				.when(employeeTaskService).delete(1L);

		mockMvc.perform(delete("/api/tasks/1").with(user("bob").roles("USER")))
				.andExpect(status().isForbidden());
	}
}
