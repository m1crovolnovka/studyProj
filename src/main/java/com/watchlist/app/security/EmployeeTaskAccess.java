package com.watchlist.app.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.EmployeeTask;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.repository.EmployeeRepository;

@Component("employeeTaskAccess")
public class EmployeeTaskAccess {

	private final AppUserRepository appUserRepository;
	private final EmployeeRepository employeeRepository;

	public EmployeeTaskAccess(
			AppUserRepository appUserRepository,
			EmployeeRepository employeeRepository) {
		this.appUserRepository = appUserRepository;
		this.employeeRepository = employeeRepository;
	}

	public Long requireOwnEmployeeId() {
		AppUser user = requireCurrentUser();
		return employeeRepository.findByUser_Id(user.getId())
				.map(Employee::getId)
				.orElseThrow(() -> new AccessDeniedException("No employee linked to the current user"));
	}

	public AppUser requireCurrentUser() {
		Authentication authentication = currentAuthentication();
		AppUser user = currentUser(authentication);
		if (user == null) {
			throw new AccessDeniedException("Access denied");
		}
		return user;
	}

	public boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return isAdmin(authentication);
	}

	public void requireAdmin() {
		if (!isAdmin(currentAuthentication())) {
			throw new AccessDeniedException("Access denied");
		}
	}

	/**
	 * Админ может выдать задание любому работнику,
	 * пользователь — только себе.
	 */
	public void checkCanAssign(Long employeeId) {
		Authentication authentication = currentAuthentication();
		if (isAdmin(authentication)) {
			return;
		}
		if (!canAccessEmployee(authentication, employeeId)) {
			throw new AccessDeniedException("Access denied");
		}
	}

	/**
	 * Просмотр и выполнение: админ или исполнитель задания.
	 */
	public void checkAssigneeOrAdmin(EmployeeTask task) {
		Authentication authentication = currentAuthentication();
		if (isAdmin(authentication)) {
			return;
		}
		if (!canAccessEmployee(authentication, task.getEmployee().getId())) {
			throw new AccessDeniedException("Access denied");
		}
	}

	/**
	 * Удаление: админ или создатель задания.
	 */
	public void checkCreatorOrAdmin(EmployeeTask task) {
		Authentication authentication = currentAuthentication();
		if (isAdmin(authentication)) {
			return;
		}
		AppUser user = currentUser(authentication);
		if (user == null || task.getCreatedByUserId() == null
				|| !task.getCreatedByUserId().equals(user.getId())) {
			throw new AccessDeniedException("Access denied");
		}
	}

	public void checkAccess(Long employeeId) {
		checkCanAssign(employeeId);
	}

	public boolean canAccessEmployee(Long employeeId) {
		Authentication authentication =
				SecurityContextHolder.getContext().getAuthentication();
		return canAccessEmployee(authentication, employeeId);
	}

	private boolean isAdmin(Authentication authentication) {
		if (authentication == null) {
			return false;
		}
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	private boolean canAccessEmployee(
			Authentication authentication,
			Long employeeId) {

		if (authentication == null
				|| !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			return false;
		}

		AppUser user = currentUser(authentication);
		if (user == null) {
			return false;
		}

		return employeeRepository.findByUser_Id(user.getId())
				.map(employee -> employee.getId().equals(employeeId))
				.orElse(false);
	}

	private Authentication currentAuthentication() {
		Authentication authentication =
				SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new AccessDeniedException("Access denied");
		}

		return authentication;
	}

	private AppUser currentUser(Authentication authentication) {
		return appUserRepository
				.findByUsername(authentication.getName())
				.orElse(null);
	}
}
