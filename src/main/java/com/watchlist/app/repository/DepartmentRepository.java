package com.watchlist.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.watchlist.app.domain.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	Optional<Department> findByName(String name);

	boolean existsByName(String name);

	List<Department> findByLocation(String location);
}
