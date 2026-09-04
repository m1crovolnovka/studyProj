package com.watchlist.app.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Role;
import com.watchlist.app.dto.RegisterRequest;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.UsernameAlreadyExistsException;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.repository.DepartmentRepository;
import com.watchlist.app.repository.EmployeeRepository;

@Service
@Transactional
public class AuthService {

	private final AppUserRepository appUserRepository;
	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(
			AppUserRepository appUserRepository,
			EmployeeRepository employeeRepository,
			DepartmentRepository departmentRepository,
			PasswordEncoder passwordEncoder
	) {
		this.appUserRepository = appUserRepository;
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public AppUser register(RegisterRequest request) {

		if (appUserRepository.existsByUsername(request.username())) {
			throw new UsernameAlreadyExistsException(request.username());
		}

		AppUser user = new AppUser(
				request.username(),
				passwordEncoder.encode(request.password()),
				Role.USER
		);

		AppUser savedUser = appUserRepository.save(user);

		createLinkedEmployee(savedUser, request);

		return savedUser;
	}

	private void createLinkedEmployee(AppUser user, RegisterRequest request) {
		Department department = departmentRepository.findById(request.departmentId())
				.orElseThrow(() -> new DepartmentNotFoundException(request.departmentId()));

		Employee employee = new Employee();
		employee.setUser(user);
		employee.setDepartment(department);
		employee.setFirstName(request.firstName());
		employee.setLastName(request.lastName());
		employee.setEmail(request.email());
		employeeRepository.save(employee);
	}
}