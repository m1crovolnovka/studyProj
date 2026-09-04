package com.watchlist.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.watchlist.app.domain.EmployeeTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Role;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeTaskAccessTest {

	@Mock
	private AppUserRepository appUserRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	private EmployeeTaskAccess employeeTaskAccess;

	@BeforeEach
	void setUp() {
		employeeTaskAccess =
				new EmployeeTaskAccess(appUserRepository, employeeRepository);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void adminCanAccessAnyEmployee() {
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("admin", "ROLE_ADMIN"));

		employeeTaskAccess.checkAccess(99L);
	}

	@Test
	void linkedUserCanAccessOwnEmployee() {
		AppUser user = new AppUser("alice", "encoded", Role.USER);
		user.setUsername("alice");
		Employee employee = new Employee();
		employee.setId(5L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(employeeRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(employee));

		employeeTaskAccess.checkAccess(5L);
	}

	@Test
	void userCannotAccessAnotherEmployee() {
		AppUser user = new AppUser("alice", "password", Role.USER);
		Employee employee = new Employee();
		employee.setId(5L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(employeeRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(employee));

		assertThatThrownBy(() -> employeeTaskAccess.checkAccess(6L))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void unauthenticatedUserIsDenied() {
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("anonymousUser", "ROLE_ANONYMOUS"));

		assertThatThrownBy(() -> employeeTaskAccess.checkAccess(5L))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void canAccessEmployeeReturnsFalseForUnlinkedUser() {
		AppUser user = new AppUser("alice", "password", Role.USER);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(employeeRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

		assertThat(employeeTaskAccess.canAccessEmployee(5L)).isFalse();
	}

	@Test
	void assigneeOrAdminAllowsLinkedUser() {
		AppUser user = user("alice", 1L);
		Employee employee = employee(5L);
		EmployeeTask task = new EmployeeTask(employee, "task", null, 1L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(employeeRepository.findByUser_Id(1L)).thenReturn(Optional.of(employee));

		employeeTaskAccess.checkAssigneeOrAdmin(task);
	}

	@Test
	void assigneeOrAdminDeniesOtherUser() {
		AppUser user = user("alice", 1L);
		Employee employee = employee(5L);
		EmployeeTask task = new EmployeeTask(employee(6L), "task", null, 2L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(employeeRepository.findByUser_Id(1L)).thenReturn(Optional.of(employee));

		assertThatThrownBy(() -> employeeTaskAccess.checkAssigneeOrAdmin(task))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void creatorOrAdminAllowsCreator() {
		AppUser user = user("alice", 1L);
		EmployeeTask task = new EmployeeTask(employee(5L), "task", null, 1L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		employeeTaskAccess.checkCreatorOrAdmin(task);
	}

	@Test
	void creatorOrAdminDeniesNonCreator() {
		AppUser user = user("alice", 1L);
		EmployeeTask task = new EmployeeTask(employee(5L), "task", null, 99L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("alice", "ROLE_USER"));
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> employeeTaskAccess.checkCreatorOrAdmin(task))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void creatorOrAdminAllowsAdmin() {
		EmployeeTask task = new EmployeeTask(employee(5L), "task", null, 99L);
		SecurityContextHolder.getContext().setAuthentication(
				authenticated("admin", "ROLE_ADMIN"));

		employeeTaskAccess.checkCreatorOrAdmin(task);
	}

	private AppUser user(String username, Long id) {
		AppUser user = new AppUser(username, "encoded", Role.USER);
		user.setId(id);
		return user;
	}

	private Employee employee(Long id) {
		Employee employee = new Employee();
		employee.setId(id);
		return employee;
	}

	private UsernamePasswordAuthenticationToken authenticated(
			String username,
			String role) {
		return new UsernamePasswordAuthenticationToken(
				username,
				null,
				List.of(new SimpleGrantedAuthority(role)));
	}
}