package com.watchlist.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.watchlist.app.domain.Department;
import com.watchlist.app.domain.Employee;
import com.watchlist.app.domain.Position;
import com.watchlist.app.dto.EmployeeRequest;
import com.watchlist.app.dto.EmployeeResponse;
import com.watchlist.app.dto.SalarySyncResponse;
import com.watchlist.app.exception.DepartmentNotFoundException;
import com.watchlist.app.exception.EmployeeNotFoundException;
import com.watchlist.app.exception.PositionNotFoundException;
import com.watchlist.app.repository.DepartmentRepository;
import com.watchlist.app.repository.EmployeeRepository;
import com.watchlist.app.repository.PositionRepository;

@Service
@Transactional
public class EmployeeService {

	private final EmployeeRepository employeeRepository;
	private final DepartmentRepository departmentRepository;
	private final PositionRepository positionRepository;

	public EmployeeService(
			EmployeeRepository employeeRepository,
			DepartmentRepository departmentRepository,
			PositionRepository positionRepository) {
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.positionRepository = positionRepository;
	}

	@Transactional(readOnly = true)
	public List<EmployeeResponse> findByDepartmentId(Long departmentId) {
		checkDepartmentExists(departmentId);
		return employeeRepository.findByDepartmentId(departmentId).stream()
				.map(EmployeeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<EmployeeResponse> findByDepartmentIdAndPosition(Long departmentId, Long positionId) {
		checkDepartmentExists(departmentId);
		checkPositionExists(positionId);
		return employeeRepository.findByDepartmentIdAndPositionId(departmentId, positionId).stream()
				.map(EmployeeResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public EmployeeResponse findById(Long id) {
		return EmployeeResponse.from(getEmployee(id));
	}

	public EmployeeResponse create(Long departmentId, EmployeeRequest request) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new DepartmentNotFoundException(departmentId));
		Employee employee = new Employee();
		employee.setDepartment(department);
		apply(employee, request);
		return EmployeeResponse.from(employeeRepository.save(employee));
	}

	public EmployeeResponse update(Long id, EmployeeRequest request) {
		Employee employee = getEmployee(id);
		apply(employee, request);
		return EmployeeResponse.from(employeeRepository.save(employee));
	}

	@Transactional
	public SalarySyncResponse synchronizeSalaries() {
		List<Employee> employees = employeeRepository.findAll();
		employees.forEach(this::recalculateSalary);
		return new SalarySyncResponse(employees.size());
	}

	public void delete(Long id) {
		if (!employeeRepository.existsById(id)) {
			throw new EmployeeNotFoundException(id);
		}
		employeeRepository.deleteById(id);
	}

	private Employee getEmployee(Long id) {
		return employeeRepository.findById(id)
				.orElseThrow(() -> new EmployeeNotFoundException(id));
	}

	private Position getPosition(Long id) {
		return positionRepository.findById(id)
				.orElseThrow(() -> new PositionNotFoundException(id));
	}

	private void checkDepartmentExists(Long departmentId) {
		if (!departmentRepository.existsById(departmentId)) {
			throw new DepartmentNotFoundException(departmentId);
		}
	}

	private void checkPositionExists(Long positionId) {
		if (!positionRepository.existsById(positionId)) {
			throw new PositionNotFoundException(positionId);
		}
	}

	private void apply(Employee employee, EmployeeRequest request) {
		employee.setFirstName(request.firstName());
		employee.setLastName(request.lastName());
		employee.setEmail(request.email());
		employee.setPosition(getPosition(request.positionId()));
		employee.setBaseSalary(request.baseSalary());
		recalculateSalary(employee);
	}

	private void recalculateSalary(Employee employee) {
		BigDecimal baseSalary = employee.getBaseSalary();
		if (baseSalary == null) {
			baseSalary = employee.getSalary();
			employee.setBaseSalary(baseSalary);
		}

		if (baseSalary == null) {
			employee.setSalary(null);
			return;
		}

		Position position = employee.getPosition();
		if (position == null || position.getCoefficient() == null) {
			employee.setSalary(baseSalary);
			return;
		}

		employee.setSalary(baseSalary.multiply(position.getCoefficient()).setScale(2, RoundingMode.HALF_UP));
	}
}
