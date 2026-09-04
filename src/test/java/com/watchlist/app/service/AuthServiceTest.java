package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Role;
import com.watchlist.app.dto.RegisterRequest;
import com.watchlist.app.exception.UsernameAlreadyExistsException;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.repository.DepartmentRepository;
import com.watchlist.app.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private DepartmentRepository departmentRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService authService;

	@Test
	void registerSavesUserWithUserRoleAndEncodedPassword() {
		Department it = department(1L);
		when(appUserRepository.existsByUsername("alice")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(appUserRepository.save(any(AppUser.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(departmentRepository.findById(1L)).thenReturn(java.util.Optional.of(it));
		when(employeeRepository.save(any(Employee.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppUser created = authService.register(
				new RegisterRequest("alice", "password123", "Alice", "One", "alice@example.com", 1L));

		assertThat(created.getUsername()).isEqualTo("alice");
		assertThat(created.getPassword()).isEqualTo("encoded-password");
		assertThat(created.getRole()).isEqualTo(Role.USER);
		assertThat(created.isEnabled()).isTrue();
		verify(appUserRepository).save(any(AppUser.class));
	}

	@Test
	void registerCreatesLinkedEmployeeWithProvidedData() {
		Department it = department(1L);
		when(appUserRepository.existsByUsername("alice")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(appUserRepository.save(any(AppUser.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(departmentRepository.findById(1L)).thenReturn(java.util.Optional.of(it));
		when(employeeRepository.save(any(Employee.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AppUser created = authService.register(
				new RegisterRequest("alice", "password123", "Alice", "One", "alice@example.com", 1L));

		assertThat(created.getUsername()).isEqualTo("alice");
		assertThat(created.isEnabled()).isTrue();

		ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
		verify(employeeRepository).save(captor.capture());
		Employee linked = captor.getValue();
		assertThat(linked.getUser()).isEqualTo(created);
		assertThat(linked.getDepartment()).isEqualTo(it);
		assertThat(linked.getFirstName()).isEqualTo("Alice");
		assertThat(linked.getLastName()).isEqualTo("One");
		assertThat(linked.getEmail()).isEqualTo("alice@example.com");
		assertThat(linked.getPosition()).isNull();
		assertThat(linked.getBaseSalary()).isNull();
	}

	@Test
	void registerThrowsWhenUsernameAlreadyExists() {
		when(appUserRepository.existsByUsername("alice")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(
				new RegisterRequest("alice", "password123", "Alice", "One", null, 1L)))
				.isInstanceOf(UsernameAlreadyExistsException.class)
				.hasMessageContaining("alice");
		verify(appUserRepository, never()).save(any(AppUser.class));
		verify(employeeRepository, never()).save(any(Employee.class));
	}

	private Department department(Long id) {
		Department department = new Department();
		department.setId(id);
		department.setName("IT");
		department.setLocation("Office A");
		return department;
	}
}