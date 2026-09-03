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

import com.watchlist.app.config.SecurityConfig;
import com.watchlist.app.dto.EmployeeResponse;
import com.watchlist.app.dto.PositionResponse;
import com.watchlist.app.dto.SalarySyncResponse;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.service.EmployeeService;

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class EmployeeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmployeeService employeeService;

	@Test
	void getEmployeesIsPublic() throws Exception {
		when(employeeService.findByDepartmentId(1L)).thenReturn(List.of(
				new EmployeeResponse(3L, 1L, "Alice", "One", "alice@example.com",
						new PositionResponse(2L, "SENIOR", new BigDecimal("1.50")),
						new BigDecimal("100"), new BigDecimal("150.00"))));

		mockMvc.perform(get("/api/departments/1/employees"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].firstName").value("Alice"))
				.andExpect(jsonPath("$[0].position.name").value("SENIOR"));
	}

	@Test
	void getEmployeesFilteredByPosition() throws Exception {
		when(employeeService.findByDepartmentIdAndPosition(1L, 2L)).thenReturn(List.of(
				new EmployeeResponse(4L, 1L, "Bob", "Two", null,
						new PositionResponse(2L, "MIDDLE", new BigDecimal("1.20")),
						new BigDecimal("100"), new BigDecimal("120.00"))));

		mockMvc.perform(get("/api/departments/1/employees").param("positionId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].position.name").value("MIDDLE"));
	}

	@Test
	void getEmployeeByIdIsPublic() throws Exception {
		when(employeeService.findById(3L)).thenReturn(
				new EmployeeResponse(3L, 1L, "Alice", "One", null,
						new PositionResponse(2L, "SENIOR", new BigDecimal("1.50")),
						new BigDecimal("100"), new BigDecimal("150.00")));

		mockMvc.perform(get("/api/employees/3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Alice"));
	}

	@Test
	void createRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/departments/1/employees")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Alice","lastName":"One","positionId":2,"baseSalary":100}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createWithAuth() throws Exception {
		when(employeeService.create(eq(1L), any())).thenReturn(
				new EmployeeResponse(5L, 1L, "Alice", "One", null,
						new PositionResponse(2L, "SENIOR", new BigDecimal("1.50")),
						new BigDecimal("100"), new BigDecimal("150.00")));

		mockMvc.perform(post("/api/departments/1/employees")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Alice","lastName":"One","positionId":2,"baseSalary":100}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/departments/1/employees/5"))
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.salary").value(150.00));
	}

	@Test
	void createRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/departments/1/employees")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"","lastName":"","baseSalary":100}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createThrowsWhenDepartmentMissing() throws Exception {
		doThrow(new DepartmentNotFoundException(99L)).when(employeeService).create(eq(99L), any());

		mockMvc.perform(post("/api/departments/99/employees")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Alice","lastName":"One","positionId":2,"baseSalary":100}
						"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void updateWithAuth() throws Exception {
		when(employeeService.update(eq(2L), any())).thenReturn(
				new EmployeeResponse(2L, 1L, "Alice", "One", null,
						new PositionResponse(4L, "LEAD", new BigDecimal("1.80")),
						new BigDecimal("100"), new BigDecimal("180.00")));

		mockMvc.perform(put("/api/employees/2")
				.with(user("admin"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"Alice","lastName":"One","positionId":4,"baseSalary":100}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.position.name").value("LEAD"))
				.andExpect(jsonPath("$.salary").value(180.00));
	}

	@Test
	void synchronizeSalariesRequiresAuth() throws Exception {
		mockMvc.perform(post("/api/employees/sync-salaries"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void synchronizeSalariesWithAuth() throws Exception {
		when(employeeService.synchronizeSalaries()).thenReturn(new SalarySyncResponse(3));

		mockMvc.perform(post("/api/employees/sync-salaries").with(user("admin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.synchronizedEmployees").value(3));
	}

	@Test
	void deleteMissingEmployee() throws Exception {
		doThrow(new EmployeeNotFoundException(99L)).when(employeeService).delete(99L);

		mockMvc.perform(delete("/api/employees/99").with(user("admin")))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteExistingEmployee() throws Exception {
		doNothing().when(employeeService).delete(1L);

		mockMvc.perform(delete("/api/employees/1").with(user("admin")))
				.andExpect(status().isNoContent());
	}
}
