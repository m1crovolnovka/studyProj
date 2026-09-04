package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.domain.Role;
import com.watchlist.app.domain.TaskStatus;
import com.watchlist.app.dto.EmployeeTaskRequest;
import com.watchlist.app.dto.EmployeeTaskStatusUpdateRequest;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.exception.EmployeeTaskNotFoundException;
import com.watchlist.app.repository.EmployeeRepository;
import com.watchlist.app.repository.EmployeeTaskRepository;
import com.watchlist.app.security.EmployeeTaskAccess;

@ExtendWith(MockitoExtension.class)
class EmployeeTaskServiceTest {

	@Mock
	private EmployeeTaskRepository employeeTaskRepository;

	@Mock
	private EmployeeRepository employeeRepository;

	@Mock
	private EmployeeTaskAccess employeeTaskAccess;

	@InjectMocks
	private EmployeeTaskService employeeTaskService;

	private final Employee employee = employee(5L);
	private final AppUser creator = user(1L, "alice");

	@Test
	void createSavesPendingTaskAndReturnsResponse() {
		when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
		when(employeeTaskAccess.requireCurrentUser()).thenReturn(creator);
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> {
			EmployeeTask task = invocation.getArgument(0);
			task.setId(10L);
			return task;
		});

		var created = employeeTaskService.create(5L,
				new EmployeeTaskRequest("Write report", "Quarterly report"));

		assertThat(created.id()).isEqualTo(10L);
		assertThat(created.employeeId()).isEqualTo(5L);
		assertThat(created.title()).isEqualTo("Write report");
		assertThat(created.description()).isEqualTo("Quarterly report");
		assertThat(created.status()).isEqualTo(TaskStatus.PENDING);
		assertThat(created.completedAt()).isNull();
		verify(employeeTaskAccess).checkCanAssign(5L);
	}

	@Test
	void createOwnUsesLinkedEmployee() {
		when(employeeTaskAccess.requireOwnEmployeeId()).thenReturn(5L);
		when(employeeRepository.findById(5L)).thenReturn(Optional.of(employee));
		when(employeeTaskAccess.requireCurrentUser()).thenReturn(creator);
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> {
			EmployeeTask task = invocation.getArgument(0);
			task.setId(10L);
			return task;
		});

		var created = employeeTaskService.createOwn(new EmployeeTaskRequest("Own task", null));

		assertThat(created.employeeId()).isEqualTo(5L);
		assertThat(created.title()).isEqualTo("Own task");
	}

	@Test
	void createThrowsWhenEmployeeMissing() {
		when(employeeRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> employeeTaskService.create(7L,
				new EmployeeTaskRequest("task", null)))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("7");
	}

	@Test
	void findByEmployeeIdThrowsWhenEmployeeMissing() {
		when(employeeRepository.existsById(9L)).thenReturn(false);

		assertThatThrownBy(() -> employeeTaskService.findByEmployeeId(9L, null))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("9");
		verify(employeeTaskAccess).requireAdmin();
	}

	@Test
	void findOwnDelegatesToLinkedEmployee() {
		when(employeeTaskAccess.requireOwnEmployeeId()).thenReturn(5L);
		when(employeeRepository.existsById(5L)).thenReturn(true);
		when(employeeTaskRepository.findByEmployeeId(5L)).thenReturn(List.of());

		assertThat(employeeTaskService.findOwn(null)).isEmpty();
		verify(employeeTaskAccess).requireOwnEmployeeId();
	}

	@Test
	void findByIdThrowsWhenTaskMissing() {
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> employeeTaskService.findById(10L))
				.isInstanceOf(EmployeeTaskNotFoundException.class)
				.hasMessageContaining("10");
	}

	@Test
	void findByIdDeniedForNonAssignee() {
		EmployeeTask foreignTask = new EmployeeTask(employee(6L), "other", null, 99L);
		foreignTask.setId(10L);
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(foreignTask));
		doThrow(new AccessDeniedException("Access denied"))
				.when(employeeTaskAccess).checkAssigneeOrAdmin(foreignTask);

		assertThatThrownBy(() -> employeeTaskService.findById(10L))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void updateStatusCompletesTaskAndSetsCompletedAt() {
		EmployeeTask task = new EmployeeTask(employee, "Fix bug", null, 1L);
		task.setId(10L);
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Instant before = Instant.now();
		var result = employeeTaskService.updateStatus(10L,
				new EmployeeTaskStatusUpdateRequest(TaskStatus.COMPLETED));

		assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(result.completedAt()).isNotNull();
		assertThat(result.completedAt()).isAfterOrEqualTo(before);
		verify(employeeTaskAccess).checkAssigneeOrAdmin(task);
	}

	@Test
	void updateStatusReopensCompletedTaskAndClearsCompletedAt() {
		EmployeeTask task = new EmployeeTask(employee, "Fix bug", null, 1L);
		task.setId(10L);
		task.setStatus(TaskStatus.COMPLETED);
		task.setCompletedAt(Instant.now());
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = employeeTaskService.updateStatus(10L,
				new EmployeeTaskStatusUpdateRequest(TaskStatus.IN_PROGRESS));

		assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
		assertThat(result.completedAt()).isNull();
	}

	@Test
	void ownStatsUsesLinkedEmployee() {
		when(employeeTaskAccess.requireOwnEmployeeId()).thenReturn(5L);
		when(employeeRepository.existsById(5L)).thenReturn(true);
		when(employeeTaskRepository.countByEmployeeId(5L)).thenReturn(4L);
		when(employeeTaskRepository.countByEmployeeIdAndStatus(5L, TaskStatus.PENDING)).thenReturn(1L);
		when(employeeTaskRepository.countByEmployeeIdAndStatus(5L, TaskStatus.IN_PROGRESS)).thenReturn(1L);
		when(employeeTaskRepository.countByEmployeeIdAndStatus(5L, TaskStatus.COMPLETED)).thenReturn(2L);

		var stats = employeeTaskService.ownStats();

		assertThat(stats.total()).isEqualTo(4);
		assertThat(stats.pending()).isEqualTo(1);
		assertThat(stats.inProgress()).isEqualTo(1);
		assertThat(stats.completed()).isEqualTo(2);
	}

	@Test
	void completeMarksPendingTaskAsCompleted() {
		EmployeeTask task = new EmployeeTask(employee, "Fix bug", null, 1L);
		task.setId(10L);
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Instant before = Instant.now();
		var result = employeeTaskService.complete(10L);

		assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(result.completedAt()).isNotNull();
		assertThat(result.completedAt()).isAfterOrEqualTo(before);
		verify(employeeTaskAccess).checkAssigneeOrAdmin(task);
	}

	@Test
	void completeKeepsCompletedAtForAlreadyCompletedTask() {
		EmployeeTask task = new EmployeeTask(employee, "Fix bug", null, 1L);
		task.setId(10L);
		task.setStatus(TaskStatus.COMPLETED);
		task.setCompletedAt(Instant.parse("2026-01-01T10:00:00Z"));
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(task));
		when(employeeTaskRepository.save(any(EmployeeTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var result = employeeTaskService.complete(10L);

		assertThat(result.status()).isEqualTo(TaskStatus.COMPLETED);
		assertThat(result.completedAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
	}

	@Test
	void deleteRemovesTaskWhenCreatorOrAdmin() {
		EmployeeTask task = new EmployeeTask(employee, "Fix bug", null, 1L);
		task.setId(10L);
		when(employeeTaskRepository.findById(10L)).thenReturn(Optional.of(task));

		employeeTaskService.delete(10L);

		verify(employeeTaskAccess).checkCreatorOrAdmin(task);
		verify(employeeTaskRepository).delete(task);
	}

	private Employee employee(Long id) {
		Employee employee = new Employee();
		employee.setId(id);
		employee.setFirstName("Alice");
		employee.setLastName("One");
		return employee;
	}

	private AppUser user(Long id, String username) {
		AppUser user = new AppUser(username, "encoded", Role.USER);
		user.setId(id);
		return user;
	}
}
