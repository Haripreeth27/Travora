package com.traveldesk.backend.employee;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.common.PaginatedResponse;
import com.traveldesk.backend.company.Company;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "employeeId",
            "name",
            "department",
            "designation",
            "location",
            "email",
            "active",
            "createdAt",
            "updatedAt"
    );

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

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

    private User getAuthenticatedUser() {
        return getCurrentUserOptional().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated")
        );
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: '" + property + "'");
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            String dirStr = parts[1].trim().toLowerCase();
            if ("desc".equals(dirStr)) {
                direction = Sort.Direction.DESC;
            } else if (!"asc".equals(dirStr)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction: must be 'asc' or 'desc'");
            }
        }
        return Sort.by(direction, property);
    }

    private Pageable validateAndBuildPageable(int page, int size, String sortParam) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must not be less than zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        Sort sort = parseSort(sortParam);
        return PageRequest.of(page, size, sort);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PaginatedResponse<EmployeeResponse> listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "name,asc") String sort
    ) {
        User currentUser = getAuthenticatedUser();
        Pageable pageable = validateAndBuildPageable(page, size, sort);
        String cleanedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        // Role-based visibility enforcement
        if (currentUser.getRole() == Role.EMPLOYEE) {
            // Employees can only access their own linked profile
            String empId = currentUser.getEmployeeId();
            if (empId == null || currentUser.getCompany() == null) {
                return new PaginatedResponse<>(java.util.List.of(), page, size, 0, 0, true, true);
            }
            Page<Employee> selfPage = employeeRepository.findByCompanyIdAndEmployeeId(
                    currentUser.getCompany().getId(),
                    empId,
                    pageable
            );
            return PaginatedResponse.from(selfPage.map(EmployeeResponse::fromEntity));
        }

        // Platform Admin can view all or tenant-scoped
        if (currentUser.getRole() == Role.PLATFORM_ADMIN) {
            Long companyId = currentUser.getCompany() != null ? currentUser.getCompany().getId() : null;
            Page<Employee> paged = employeeRepository.searchEmployeesPlatformAdmin(companyId, active, cleanedSearch, pageable);
            return PaginatedResponse.from(paged.map(EmployeeResponse::fromEntity));
        }

        // Company-level roles: COMPANY_ADMIN, ADMIN, TRAVEL_DESK, APPROVER
        if (currentUser.getCompany() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not associated with an active company");
        }

        Page<Employee> paged = employeeRepository.searchEmployees(
                currentUser.getCompany().getId(),
                active,
                cleanedSearch,
                pageable
        );
        return PaginatedResponse.from(paged.map(EmployeeResponse::fromEntity));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse getCurrentEmployee() {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee record linked to current user account");
        }
        if (currentUser.getCompany() != null) {
            return employeeRepository.findByCompanyIdAndEmployeeId(currentUser.getCompany().getId(), currentUser.getEmployeeId())
                    .map(EmployeeResponse::fromEntity)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile not found"));
        }
        return employeeRepository.findByEmployeeId(currentUser.getEmployeeId())
                .map(EmployeeResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile not found"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse getEmployeeById(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        // Tenant boundary check
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            if (employee.getCompany() == null || !employee.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
            }
        }

        // Employee self-access only
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (currentUser.getEmployeeId() == null || !employee.getEmployeeId().equals(currentUser.getEmployeeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own employee profile");
            }
        }

        return EmployeeResponse.fromEntity(employee);
    }

    @GetMapping("/employee-id/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse getEmployeeByEmployeeId(@PathVariable String employeeId) {
        User currentUser = getAuthenticatedUser();

        // Employee self-access only
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (currentUser.getEmployeeId() == null || !employeeId.equals(currentUser.getEmployeeId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own employee profile");
            }
        }

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            return employeeRepository.findByCompanyIdAndEmployeeId(currentUser.getCompany().getId(), employeeId)
                    .map(EmployeeResponse::fromEntity)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        }

        return employeeRepository.findByEmployeeId(employeeId)
                .map(EmployeeResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        User currentUser = getAuthenticatedUser();
        Company targetCompany = currentUser.getCompany();

        if (targetCompany == null && currentUser.getRole() != Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create employee without an associated company");
        }

        String cleanEmpId = request.getEmployeeId().trim();
        Long companyId = targetCompany != null ? targetCompany.getId() : null;

        if (companyId != null && employeeRepository.existsByCompanyIdAndEmployeeId(companyId, cleanEmpId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Employee ID '" + cleanEmpId + "' already exists in this organization"
            );
        }

        Employee emp = new Employee();
        emp.setCompany(targetCompany);
        emp.setEmployeeId(cleanEmpId);
        emp.setName(request.getName().trim());
        emp.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        emp.setDesignation(request.getDesignation() != null ? request.getDesignation().trim() : null);
        emp.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        emp.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        emp.setContactNumber(request.getContactNumber() != null ? request.getContactNumber().trim() : null);
        emp.setActive(request.getActive() != null ? request.getActive() : true);

        Employee saved = employeeRepository.save(emp);
        return EmployeeResponse.fromEntity(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public EmployeeResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        User currentUser = getAuthenticatedUser();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            if (employee.getCompany() == null || !employee.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
            }
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            employee.setName(request.getName().trim());
        }
        if (request.getDepartment() != null) {
            employee.setDepartment(request.getDepartment().trim());
        }
        if (request.getDesignation() != null) {
            employee.setDesignation(request.getDesignation().trim());
        }
        if (request.getLocation() != null) {
            employee.setLocation(request.getLocation().trim());
        }
        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getContactNumber() != null) {
            employee.setContactNumber(request.getContactNumber().trim());
        }
        if (request.getActive() != null) {
            employee.setActive(request.getActive());
        }

        Employee saved = employeeRepository.save(employee);
        return EmployeeResponse.fromEntity(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployee(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            if (employee.getCompany() == null || !employee.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
            }
        }

        // Soft-deactivate to preserve historical travel and audit records
        employee.setActive(false);
        employeeRepository.save(employee);
    }
}
