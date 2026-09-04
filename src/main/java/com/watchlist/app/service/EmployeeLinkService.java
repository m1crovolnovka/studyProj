package com.watchlist.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.dto.EmployeeLinkResponse;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.exception.UserNotFoundException;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.repository.EmployeeRepository;

@Service
@Transactional
public class EmployeeLinkService {

	private final EmployeeRepository employeeRepository;
	private final AppUserRepository appUserRepository;

	public EmployeeLinkService(
			EmployeeRepository employeeRepository,
			AppUserRepository appUserRepository) {
		this.employeeRepository = employeeRepository;
		this.appUserRepository = appUserRepository;
	}

	public EmployeeLinkResponse link(Long employeeId, Long userId) {
		Employee employee = employeeRepository.findById(employeeId)
				.orElseThrow(() -> new EmployeeNotFoundException(employeeId));

		AppUser user = appUserRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException(userId));

		employee.setUser(user);
		employeeRepository.save(employee);

		return new EmployeeLinkResponse(
				employee.getId(),
				user.getId(),
				user.getUsername(),
				employee.getFirstName(),
				employee.getLastName());
	}
}