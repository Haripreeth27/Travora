package com.traveldesk.backend.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmployeeId(String employeeId);

    java.util.List<Employee> findByCompanyId(Long companyId);

    Optional<Employee> findByCompanyIdAndEmployeeId(Long companyId, String employeeId);

    boolean existsByEmployeeId(String employeeId);
}