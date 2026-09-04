package com.watchlist.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.domain.TaskStatus;

public interface EmployeeTaskRepository extends JpaRepository<EmployeeTask, Long> {

	List<EmployeeTask> findByEmployeeId(Long employeeId);

	List<EmployeeTask> findByEmployeeIdAndStatus(Long employeeId, TaskStatus status);

	long countByEmployeeId(Long employeeId);

	long countByEmployeeIdAndStatus(Long employeeId, TaskStatus status);
}