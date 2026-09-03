package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.watchlist.app.domain.Department;
import com.watchlist.app.dto.DepartmentRequest;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.DuplicateDepartmentException;
import com.watchlist.app.repository.DepartmentRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

	@Mock
	private DepartmentRepository departmentRepository;

	@InjectMocks
	private DepartmentService departmentService;

	@Test
	void createSavesDepartment() {
		when(departmentRepository.existsByName("IT")).thenReturn(false);
		when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
			Department department = invocation.getArgument(0);
			department.setId(1L);
			return department;
		});

		var created = departmentService.create(new DepartmentRequest("IT", "Office A"));

		assertThat(created.id()).isEqualTo(1L);
		assertThat(created.name()).isEqualTo("IT");
		assertThat(created.location()).isEqualTo("Office A");
	}

	@Test
	void createThrowsWhenDuplicateName() {
		when(departmentRepository.existsByName("IT")).thenReturn(true);

		assertThatThrownBy(() -> departmentService.create(new DepartmentRequest("IT", null)))
				.isInstanceOf(DuplicateDepartmentException.class)
				.hasMessageContaining("IT");
	}

	@Test
	void findByIdThrowsWhenMissing() {
		when(departmentRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> departmentService.findById(7L))
				.isInstanceOf(DepartmentNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void updateRejectsDuplicateNameOfAnother() {
		Department existing = new Department();
		existing.setId(1L);
		existing.setName("IT");
		when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(departmentRepository.existsByName("Sales")).thenReturn(true);

		assertThatThrownBy(() -> departmentService.update(1L, new DepartmentRequest("Sales", null)))
				.isInstanceOf(DuplicateDepartmentException.class)
				.hasMessageContaining("Sales");
	}

	@Test
	void findAllReturnsDepartments() {
		Department d = new Department();
		d.setId(1L);
		d.setName("IT");
		when(departmentRepository.findAll()).thenReturn(List.of(d));

		var result = departmentService.findAll();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).name()).isEqualTo("IT");
		verify(departmentRepository).findAll();
	}
}
