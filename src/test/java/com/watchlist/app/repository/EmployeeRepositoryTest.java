package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Position;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Test
	void savesAndFiltersEmployeesByDepartment() {
		Department it = departmentRepository.save(department("IT", "Office A"));
		Department hr = departmentRepository.save(department("HR", "Office B"));
		Position senior = positionRepository.save(position("SENIOR", "1.50"));
		Position middle = positionRepository.save(position("MIDDLE", "1.20"));
		Position manager = positionRepository.save(position("MANAGER", "2.00"));

		employeeRepository.save(employee(it, senior, "Alice", "One", "100", "150.00"));
		employeeRepository.save(employee(it, middle, "Bob", "Two", "100", "120.00"));
		employeeRepository.save(employee(hr, manager, "Carol", "Three", "100", "200.00"));

		assertThat(employeeRepository.findByDepartmentId(it.getId())).hasSize(2);
		assertThat(employeeRepository.findByDepartmentIdAndPositionId(it.getId(), middle.getId()))
				.extracting(Employee::getFirstName)
				.containsExactly("Bob");
		assertThat(employeeRepository.countByDepartmentId(hr.getId())).isEqualTo(1);
	}

	private Department department(String name, String location) {
		Department department = new Department();
		department.setName(name);
		department.setLocation(location);
		return department;
	}

	private Position position(String name, String coefficient) {
		Position position = new Position();
		position.setName(name);
		position.setCoefficient(new BigDecimal(coefficient));
		return position;
	}

	private Employee employee(
			Department department,
			Position position,
			String firstName,
			String lastName,
			String baseSalary,
			String salary) {
		Employee employee = new Employee();
		employee.setDepartment(department);
		employee.setPosition(position);
		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setBaseSalary(new BigDecimal(baseSalary));
		employee.setSalary(new BigDecimal(salary));
		return employee;
	}
}
