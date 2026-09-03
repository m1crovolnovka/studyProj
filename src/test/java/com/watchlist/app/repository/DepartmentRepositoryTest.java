package com.watchlist.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.Department;

@DataJpaTest
@ActiveProfiles("test")
class DepartmentRepositoryTest {

	@Autowired
	private DepartmentRepository departmentRepository;

	@Test
	void savesAndFindsDepartments() {
		Department it = departmentRepository.save(department("IT", "Office A"));
		departmentRepository.save(department("HR", "Office B"));

		assertThat(departmentRepository.findAll()).hasSize(2);
		assertThat(departmentRepository.findByName("IT")).isPresent()
				.get()
				.extracting(Department::getId)
				.isEqualTo(it.getId());
		assertThat(departmentRepository.existsByName("HR")).isTrue();
		assertThat(departmentRepository.existsByName("Sales")).isFalse();
		assertThat(departmentRepository.findByLocation("Office A"))
				.extracting(Department::getName)
				.containsExactly("IT");
	}

	private Department department(String name, String location) {
		Department department = new Department();
		department.setName(name);
		department.setLocation(location);
		return department;
	}
}
