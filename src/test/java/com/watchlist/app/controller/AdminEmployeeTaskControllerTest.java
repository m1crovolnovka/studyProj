package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(controllers = AdminEmployeeTaskController.class)
@AutoConfigureMockMvc
@Import({ TestSecurityConfig.class, ApiExceptionHandler.class })
class AdminEmployeeTaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeTaskService employeeTaskService;

	private final EmployeeTaskResponse taskResponse = new EmployeeTaskResponse(
			10L, 5L, "Write report", "Quarterly", TaskStatus.PENDING,
			Instant.parse("2026-09-04T08:00:00Z"), null);

	@Test
	void getEmployeeTasksRequiresAdmin() throws Exception {
		mockMvc.perform(get("/api/employees/5/tasks").with(user("bob").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void getEmployeeTasksAsAdmin() throws Exception {
		when(employeeTaskService.findByEmployeeId(5L, null)).thenReturn(List.of(taskResponse));

		mockMvc.perform(get("/api/employees/5/tasks").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Write report"));
	}

	@Test
	void getEmployeeStatsAsAdmin() throws Exception {
		when(employeeTaskService.adminStats(5L)).thenReturn(new EmployeeTaskStats(3, 1, 1, 1));

		mockMvc.perform(get("/api/employees/5/tasks/stats").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(3));
	}

	@Test
	void getEmployeeStatsForbiddenForUser() throws Exception {
		mockMvc.perform(get("/api/employees/5/tasks/stats").with(user("bob").roles("USER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void createTaskForEmployeeAsAdmin() throws Exception {
		EmployeeTaskResponse created = new EmployeeTaskResponse(
				1L, 5L, "Fix bug", "Fix the login bug", TaskStatus.PENDING,
				Instant.parse("2026-09-04T08:00:00Z"), null);
		when(employeeTaskService.create(eq(5L), any())).thenReturn(created);

		mockMvc.perform(post("/api/employees/5/tasks")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Fix bug","description":"Fix the login bug"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/tasks/1"))
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void createTaskForSelfViaEmployeePath() throws Exception {
		EmployeeTaskResponse created = new EmployeeTaskResponse(
				1L, 5L, "Сделать отчёт", "Описание", TaskStatus.PENDING,
				Instant.parse("2026-09-04T08:00:00Z"), null);
		when(employeeTaskService.create(eq(5L), any())).thenReturn(created);

		mockMvc.perform(post("/api/employees/5/tasks")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Сделать отчёт","description":"Описание"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void createTaskForbiddenForOtherEmployee() throws Exception {
		doThrow(new AccessDeniedException("Access denied"))
				.when(employeeTaskService).create(eq(5L), any());

		mockMvc.perform(post("/api/employees/5/tasks")
				.with(user("bob").roles("USER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Fix bug"}
						"""))
				.andExpect(status().isForbidden());
	}
}
