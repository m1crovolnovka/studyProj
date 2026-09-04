package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.domain.Position;
import com.watchlist.app.domain.Role;
import com.watchlist.app.domain.TaskStatus;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeTaskRepositoryTest {

	@Autowired
	private EmployeeTaskRepository employeeTaskRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private PositionRepository positionRepository;

	@Autowired
	private AppUserRepository appUserRepository;

	@Test
	void savesAndFiltersTasksByEmployeeAndStatus() {
		Department it = departmentRepository.save(department("IT", "Office A"));
		Position senior = positionRepository.save(position("SENIOR", "1.50"));
		Employee alice = employeeRepository.save(employee(it, senior, "Alice", "One"));
		Employee bob = employeeRepository.save(employee(it, senior, "Bob", "Two"));

		employeeTaskRepository.save(task(alice, "Write report", TaskStatus.COMPLETED));
		employeeTaskRepository.save(task(alice, "Fix bug", TaskStatus.IN_PROGRESS));
		employeeTaskRepository.save(task(bob, "Code review", TaskStatus.PENDING));

		assertThat(employeeTaskRepository.findByEmployeeId(alice.getId())).hasSize(2);
		assertThat(employeeTaskRepository.findByEmployeeIdAndStatus(alice.getId(), TaskStatus.IN_PROGRESS))
				.extracting(EmployeeTask::getTitle)
				.containsExactly("Fix bug");
		assertThat(employeeTaskRepository.countByEmployeeId(alice.getId())).isEqualTo(2);
		assertThat(employeeTaskRepository.countByEmployeeIdAndStatus(alice.getId(), TaskStatus.COMPLETED))
				.isEqualTo(1);
		assertThat(employeeTaskRepository.countByEmployeeIdAndStatus(bob.getId(), TaskStatus.PENDING))
				.isEqualTo(1);
	}

	@Test
	void employeeLinkedToUserCanBeFoundByUserId() {
		Department it = departmentRepository.save(department("IT", "Office A"));
		Position senior = positionRepository.save(position("SENIOR", "1.50"));
		AppUser user = appUserRepository.save(new AppUser("alice", "encoded", Role.USER));
		Employee linked = employeeRepository.save(employee(it, senior, "Alice", "One"));
		linked.setUser(user);
		employeeRepository.save(linked);

		assertThat(employeeRepository.findByUser_Id(user.getId()))
				.isPresent()
				.get()
				.extracting(Employee::getFirstName)
				.isEqualTo("Alice");
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

	private Employee employee(Department department, Position position, String firstName, String lastName) {
		Employee employee = new Employee();
		employee.setDepartment(department);
		employee.setPosition(position);
		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		return employee;
	}

	private EmployeeTask task(Employee employee, String title, TaskStatus status) {
		EmployeeTask task = new EmployeeTask(employee, title, "desc", 1L);
		task.setStatus(status);
		if (status == TaskStatus.COMPLETED) {
			task.setCompletedAt(java.time.Instant.now());
		}
		return task;
	}
}