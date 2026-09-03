package com.watchlist.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

	List<Employee> findByDepartmentId(Long departmentId);

	List<Employee> findByDepartmentIdAndPositionId(Long departmentId, Long positionId);

	long countByDepartmentId(Long departmentId);
}
