package com.watchlist.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Department;
import com.watchlist.app.dto.DepartmentRequest;
import com.watchlist.app.dto.DepartmentResponse;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.DuplicateDepartmentException;
import com.watchlist.app.repository.DepartmentRepository;

@Service
@Transactional
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	public DepartmentService(DepartmentRepository departmentRepository) {
		this.departmentRepository = departmentRepository;
	}

	@Transactional(readOnly = true)
	public List<DepartmentResponse> findAll() {
		return departmentRepository.findAll().stream()
				.map(DepartmentResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public DepartmentResponse findById(Long id) {
		return DepartmentResponse.from(getDepartment(id));
	}

	public DepartmentResponse create(DepartmentRequest request) {
		if (departmentRepository.existsByName(request.name())) {
			throw new DuplicateDepartmentException(request.name());
		}
		Department department = new Department();
		department.setName(request.name());
		department.setLocation(request.location());
		return DepartmentResponse.from(departmentRepository.save(department));
	}

	public DepartmentResponse update(Long id, DepartmentRequest request) {
		Department department = getDepartment(id);
		if (!department.getName().equals(request.name())
				&& departmentRepository.existsByName(request.name())) {
			throw new DuplicateDepartmentException(request.name());
		}
		department.setName(request.name());
		department.setLocation(request.location());
		return DepartmentResponse.from(departmentRepository.save(department));
	}

	public void delete(Long id) {
		if (!departmentRepository.existsById(id)) {
			throw new DepartmentNotFoundException(id);
		}
		departmentRepository.deleteById(id);
	}

	private Department getDepartment(Long id) {
		return departmentRepository.findById(id)
				.orElseThrow(() -> new DepartmentNotFoundException(id));
	}
}
