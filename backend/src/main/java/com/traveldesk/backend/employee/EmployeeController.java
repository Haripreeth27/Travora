package com.traveldesk.backend.employee;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeController(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    private Optional<User> getCurrentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(auth.getName());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Employee> getAllEmployees() {
        Optional<User> userOpt = getCurrentUserOptional();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null) {
                return employeeRepository.findByCompanyId(user.getCompany().getId());
            }
        }
        return employeeRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Employee getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        Optional<User> userOpt = getCurrentUserOptional();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null && employee.getCompany() != null) {
                if (!employee.getCompany().getId().equals(user.getCompany().getId())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
                }
            }
            if (user.getRole() == Role.EMPLOYEE) {
                if (user.getEmployeeId() == null || !employee.getEmployeeId().equals(user.getEmployeeId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own employee profile");
                }
            }
        }

        return employee;
    }

    @GetMapping("/employee-id/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public Employee getEmployeeByEmployeeId(
            @PathVariable String employeeId) {
        Optional<User> userOpt = getCurrentUserOptional();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() == Role.EMPLOYEE) {
                if (user.getEmployeeId() == null || !employeeId.equals(user.getEmployeeId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own employee profile");
                }
            }
            if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null) {
                return employeeRepository.findByCompanyIdAndEmployeeId(user.getCompany().getId(), employeeId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
            }
        }
        return employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@RequestBody Employee employee) {
        Optional<User> userOpt = getCurrentUserOptional();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() != Role.PLATFORM_ADMIN && user.getCompany() != null) {
                employee.setCompany(user.getCompany());
            } else if (employee.getCompany() == null && user.getCompany() != null) {
                employee.setCompany(user.getCompany());
            }
        }
        return employeeRepository.save(employee);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public Employee updateEmployee(
            @PathVariable Long id,
            @RequestBody Employee updatedEmployee) {

        Employee employee = getEmployeeById(id);

        employee.setEmployeeId(updatedEmployee.getEmployeeId());
        employee.setName(updatedEmployee.getName());
        employee.setDepartment(updatedEmployee.getDepartment());
        employee.setDesignation(updatedEmployee.getDesignation());
        employee.setLocation(updatedEmployee.getLocation());

        return employeeRepository.save(employee);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        Employee employee = getEmployeeById(id);
        employeeRepository.delete(employee);
    }
}