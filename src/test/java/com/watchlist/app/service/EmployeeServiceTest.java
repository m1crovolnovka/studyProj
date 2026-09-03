package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Position;
import com.watchlist.app.dto.EmployeeRequest;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.repository.DepartmentRepository;
import com.watchlist.app.repository.EmployeeRepository;
import com.watchlist.app.repository.PositionRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private DepartmentRepository departmentRepository;

	@Mock
	private PositionRepository positionRepository;

	@InjectMocks
	private EmployeeService employeeService;

	@Test
	void createSetsDepartmentRecalculatesSalaryAndReturnsResponse() {
		Department it = new Department();
		it.setId(1L);
		Position senior = position(2L, "SENIOR", "1.50");
		when(departmentRepository.findById(1L)).thenReturn(Optional.of(it));
		when(positionRepository.findById(2L)).thenReturn(Optional.of(senior));
		when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
			Employee employee = invocation.getArgument(0);
			employee.setId(11L);
			return employee;
		});

		var created = employeeService.create(1L,
				new EmployeeRequest("Alice", "One", "alice@example.com", 2L, new BigDecimal("100")));

		assertThat(created.id()).isEqualTo(11L);
		assertThat(created.departmentId()).isEqualTo(1L);
		assertThat(created.position().id()).isEqualTo(2L);
		assertThat(created.position().name()).isEqualTo("SENIOR");
		assertThat(created.baseSalary()).isEqualByComparingTo("100");
		assertThat(created.salary()).isEqualByComparingTo("150.00");
	}

	@Test
	void createThrowsWhenDepartmentMissing() {
		when(departmentRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> employeeService.create(7L,
				new EmployeeRequest("Alice", "One", null, 2L, new BigDecimal("100"))))
				.isInstanceOf(DepartmentNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(employeeRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> employeeService.findById(7L))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void findByDepartmentIdThrowsWhenDepartmentMissing() {
		when(departmentRepository.existsById(9L)).thenReturn(false);

		assertThatThrownBy(() -> employeeService.findByDepartmentId(9L))
				.isInstanceOf(DepartmentNotFoundException.class)
				.hasMessageContaining("9");
	}

	@Test
	void findByDepartmentIdReturnsEmployees() {
		Department it = new Department();
		it.setId(1L);
		Employee emp = new Employee();
		emp.setId(5L);
		emp.setDepartment(it);
		emp.setFirstName("Alice");
		when(departmentRepository.existsById(1L)).thenReturn(true);
		when(employeeRepository.findByDepartmentId(1L)).thenReturn(List.of(emp));

		var result = employeeService.findByDepartmentId(1L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).firstName()).isEqualTo("Alice");
		verify(employeeRepository).findByDepartmentId(1L);
	}

	@Test
	void synchronizeSalariesRecalculatesAllEmployees() {
		Position middle = position(3L, "MIDDLE", "1.20");
		Employee employee = new Employee();
		employee.setId(5L);
		employee.setPosition(middle);
		employee.setBaseSalary(new BigDecimal("100"));
		when(employeeRepository.findAll()).thenReturn(List.of(employee));

		var result = employeeService.synchronizeSalaries();

		assertThat(result.synchronizedEmployees()).isEqualTo(1);
		assertThat(employee.getSalary()).isEqualByComparingTo("120.00");
	}

	private Position position(Long id, String name, String coefficient) {
		Position position = new Position();
		position.setId(id);
		position.setName(name);
		position.setCoefficient(new BigDecimal(coefficient));
		return position;
	}
}
