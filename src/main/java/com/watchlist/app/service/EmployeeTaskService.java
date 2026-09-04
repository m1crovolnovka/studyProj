package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.domain.TaskStatus;
import com.watchlist.app.dto.EmployeeTaskRequest;
import com.watchlist.app.dto.EmployeeTaskResponse;
import com.watchlist.app.dto.EmployeeTaskStats;
import com.watchlist.app.dto.EmployeeTaskStatusUpdateRequest;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.exception.EmployeeTaskNotFoundException;
import com.watchlist.app.repository.EmployeeRepository;
import com.watchlist.app.repository.EmployeeTaskRepository;
import com.watchlist.app.security.EmployeeTaskAccess;

@Service
@Transactional
public class EmployeeTaskService {

	private final EmployeeTaskRepository employeeTaskRepository;
	private final EmployeeRepository employeeRepository;
	private final EmployeeTaskAccess employeeTaskAccess;

	public EmployeeTaskService(
			EmployeeTaskRepository employeeTaskRepository,
			EmployeeRepository employeeRepository,
			EmployeeTaskAccess employeeTaskAccess) {
		this.employeeTaskRepository = employeeTaskRepository;
		this.employeeRepository = employeeRepository;
		this.employeeTaskAccess = employeeTaskAccess;
	}

	@Transactional(readOnly = true)
	public List<EmployeeTaskResponse> findOwn(TaskStatus status) {
		return listByEmployee(employeeTaskAccess.requireOwnEmployeeId(), status);
	}

	@Transactional(readOnly = true)
	public List<EmployeeTaskResponse> findByEmployeeId(Long employeeId, TaskStatus status) {
		employeeTaskAccess.requireAdmin();
		return listByEmployee(employeeId, status);
	}

	@Transactional(readOnly = true)
	public EmployeeTaskResponse findById(Long taskId) {
		EmployeeTask task = getTask(taskId);
		employeeTaskAccess.checkAssigneeOrAdmin(task);
		return EmployeeTaskResponse.from(task);
	}

	public EmployeeTaskResponse createOwn(EmployeeTaskRequest request) {
		return create(employeeTaskAccess.requireOwnEmployeeId(), request);
	}

	public EmployeeTaskResponse create(Long employeeId, EmployeeTaskRequest request) {
		employeeTaskAccess.checkCanAssign(employeeId);
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new EmployeeNotFoundException(employeeId));
		AppUser creator = employeeTaskAccess.requireCurrentUser();
		EmployeeTask task = new EmployeeTask(
				employee,
				request.title(),
				request.description(),
				creator.getId());
		return EmployeeTaskResponse.from(employeeTaskRepository.save(task));
	}

	public EmployeeTaskResponse updateStatus(Long taskId, EmployeeTaskStatusUpdateRequest request) {
		EmployeeTask task = getTask(taskId);
		employeeTaskAccess.checkAssigneeOrAdmin(task);

		TaskStatus previous = task.getStatus();
		TaskStatus next = request.status();
		task.setStatus(next);

		if (next == TaskStatus.COMPLETED && previous != TaskStatus.COMPLETED) {
			task.setCompletedAt(java.time.Instant.now());
		}
		else if (next != TaskStatus.COMPLETED && previous == TaskStatus.COMPLETED) {
			task.setCompletedAt(null);
		}

		return EmployeeTaskResponse.from(employeeTaskRepository.save(task));
	}

	public EmployeeTaskResponse complete(Long taskId) {
		EmployeeTask task = getTask(taskId);
		employeeTaskAccess.checkAssigneeOrAdmin(task);

		if (task.getStatus() != TaskStatus.COMPLETED) {
			task.setStatus(TaskStatus.COMPLETED);
			if (task.getCompletedAt() == null) {
				task.setCompletedAt(java.time.Instant.now());
			}
		}

		return EmployeeTaskResponse.from(employeeTaskRepository.save(task));
	}

	@Transactional(readOnly = true)
	public EmployeeTaskStats ownStats() {
		return stats(employeeTaskAccess.requireOwnEmployeeId());
	}

	@Transactional(readOnly = true)
	public EmployeeTaskStats adminStats(Long employeeId) {
		employeeTaskAccess.requireAdmin();
		return stats(employeeId);
	}

	public void delete(Long taskId) {
		EmployeeTask task = getTask(taskId);
		employeeTaskAccess.checkCreatorOrAdmin(task);
		employeeTaskRepository.delete(task);
	}

	private EmployeeTaskStats stats(Long employeeId) {
		checkEmployeeExists(employeeId);
		return new EmployeeTaskStats(
				employeeTaskRepository.countByEmployeeId(employeeId),
				employeeTaskRepository.countByEmployeeIdAndStatus(employeeId, TaskStatus.PENDING),
				employeeTaskRepository.countByEmployeeIdAndStatus(employeeId, TaskStatus.IN_PROGRESS),
				employeeTaskRepository.countByEmployeeIdAndStatus(employeeId, TaskStatus.COMPLETED));
	}

	private List<EmployeeTaskResponse> listByEmployee(Long employeeId, TaskStatus status) {
		checkEmployeeExists(employeeId);
		List<EmployeeTask> tasks = status == null
				? employeeTaskRepository.findByEmployeeId(employeeId)
				: employeeTaskRepository.findByEmployeeIdAndStatus(employeeId, status);
		return tasks.stream().map(EmployeeTaskResponse::from).toList();
	}

	private EmployeeTask getTask(Long taskId) {
		return employeeTaskRepository.findById(taskId)
				.orElseThrow(() -> new EmployeeTaskNotFoundException(taskId));
	}

	private void checkEmployeeExists(Long employeeId) {
		if (!employeeRepository.existsById(employeeId)) {
			throw new EmployeeNotFoundException(employeeId);
		}
	}
}
