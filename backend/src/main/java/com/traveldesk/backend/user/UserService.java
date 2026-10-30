package com.traveldesk.backend.user;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================================
    // 1. CREATE USER ACCOUNT + EMPLOYEE RECORD
    // =========================================================================

    @Transactional
    public UserCreateResponse createUser(CreateUserRequest request, User currentUser) {
        validateCallerCanManageUsers(currentUser);

        Role requestedRole = request.getRole();
        validateRoleAssignment(currentUser, requestedRole);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByUsername(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with email '" + email + "' already exists"
            );
        }

        Company company = currentUser.getCompany();
        if (company == null && currentUser.getRole() != Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user is not associated with an active company"
            );
        }

        // Resolve or create Employee record
        Employee employee = resolveOrCreateEmployee(request, company);

        // Generate secure activation token and hash
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(48);

        // Create User entity
        User user = new User();
        user.setUsername(email);
        // Set an unguessable placeholder hash until password is set via activation token
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(requestedRole);
        user.setActive(request.getActive() != null ? request.getActive() : true);
        user.setCompany(company);
        user.setEmployeeId(employee.getEmployeeId());
        user.setCreatedAt(LocalDateTime.now());
        user.setActivationTokenHash(tokenHash);
        user.setActivationTokenExpiresAt(tokenExpiry);

        User savedUser = userRepository.save(user);

        UserCreateResponse response = mapToUserCreateResponse(savedUser, employee);
        response.setActivationToken(rawToken);
        response.setActivationUrl("/activate?token=" + rawToken);
        return response;
    }

    // =========================================================================
    // 2. LIST USERS (TENANT-SCOPED)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(
            User currentUser,
            String search,
            Role roleFilter,
            String statusFilter
    ) {
        validateCallerCanManageUsers(currentUser);

        List<User> users;
        if (currentUser.getRole() == Role.PLATFORM_ADMIN && currentUser.getCompany() == null) {
            users = userRepository.findAll();
        } else {
            Long companyId = currentUser.getCompany().getId();
            users = userRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        }

        // Preload employees for current company
        Long companyId = currentUser.getCompany() != null ? currentUser.getCompany().getId() : null;
        Map<String, Employee> employeeMap = companyId != null
                ? employeeRepository.findByCompanyId(companyId).stream()
                .filter(e -> e.getEmployeeId() != null)
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e, (a, b) -> a))
                : Collections.emptyMap();

        return users.stream()
                .map(u -> {
                    Employee emp = u.getEmployeeId() != null ? employeeMap.get(u.getEmployeeId()) : null;
                    return mapToUserResponse(u, emp);
                })
                .filter(u -> matchesFilter(u, search, roleFilter, statusFilter))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 3. GET USER DETAILS
    // =========================================================================

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id, User currentUser) {
        validateCallerCanManageUsers(currentUser);
        User user = findAndValidateTenantUser(id, currentUser);

        Employee employee = null;
        if (user.getEmployeeId() != null && user.getCompany() != null) {
            employee = employeeRepository
                    .findByCompanyIdAndEmployeeId(user.getCompany().getId(), user.getEmployeeId())
                    .orElse(null);
        }

        return mapToUserResponse(user, employee);
    }

    // =========================================================================
    // 4. UPDATE USER DETAILS
    // =========================================================================

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, User currentUser) {
        validateCallerCanManageUsers(currentUser);
        User user = findAndValidateTenantUser(id, currentUser);

        // Validate role change if specified
        if (request.getRole() != null && request.getRole() != user.getRole()) {
            validateRoleAssignment(currentUser, request.getRole());
            user.setRole(request.getRole());
        }

        // Update active status if specified
        if (request.getActive() != null) {
            if (currentUser.getId().equals(user.getId()) && !request.getActive()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "You cannot deactivate your own account"
                );
            }
            user.setActive(request.getActive());
        }

        // Update employee details if linked
        Employee employee = null;
        if (user.getEmployeeId() != null && user.getCompany() != null) {
            employee = employeeRepository
                    .findByCompanyIdAndEmployeeId(user.getCompany().getId(), user.getEmployeeId())
                    .orElse(null);

            if (employee != null) {
                if (request.getName() != null && !request.getName().trim().isEmpty()) {
                    employee.setName(request.getName().trim());
                }
                if (request.getDepartment() != null) {
                    employee.setDepartment(request.getDepartment().trim());
                }
                if (request.getDesignation() != null) {
                    employee.setDesignation(request.getDesignation().trim());
                }
                employeeRepository.save(employee);
            }
        }

        User saved = userRepository.save(user);
        return mapToUserResponse(saved, employee);
    }

    // =========================================================================
    // 5. UPDATE USER STATUS (ACTIVATE / DEACTIVATE)
    // =========================================================================

    @Transactional
    public UserResponse updateUserStatus(Long id, boolean active, User currentUser) {
        validateCallerCanManageUsers(currentUser);
        User user = findAndValidateTenantUser(id, currentUser);

        if (currentUser.getId().equals(user.getId()) && !active) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot deactivate your own account"
            );
        }

        user.setActive(active);
        User saved = userRepository.save(user);

        Employee employee = null;
        if (saved.getEmployeeId() != null && saved.getCompany() != null) {
            employee = employeeRepository
                    .findByCompanyIdAndEmployeeId(saved.getCompany().getId(), saved.getEmployeeId())
                    .orElse(null);
        }

        return mapToUserResponse(saved, employee);
    }

    // =========================================================================
    // 6. RESEND / REGENERATE ACTIVATION LINK
    // =========================================================================

    @Transactional
    public UserCreateResponse resendActivation(Long id, User currentUser) {
        validateCallerCanManageUsers(currentUser);
        User user = findAndValidateTenantUser(id, currentUser);

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(48);

        user.setActivationTokenHash(tokenHash);
        user.setActivationTokenExpiresAt(tokenExpiry);
        User saved = userRepository.save(user);

        Employee employee = null;
        if (saved.getEmployeeId() != null && saved.getCompany() != null) {
            employee = employeeRepository
                    .findByCompanyIdAndEmployeeId(saved.getCompany().getId(), saved.getEmployeeId())
                    .orElse(null);
        }

        UserCreateResponse response = mapToUserCreateResponse(saved, employee);
        response.setActivationToken(rawToken);
        response.setActivationUrl("/activate?token=" + rawToken);
        return response;
    }

    // =========================================================================
    // 7. VERIFY ACTIVATION TOKEN (PUBLIC)
    // =========================================================================

    @Transactional(readOnly = true)
    public TokenVerificationResponse verifyActivationToken(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return TokenVerificationResponse.invalid("Activation token is required");
        }

        String tokenHash = hashToken(rawToken.trim());
        Optional<User> userOpt = userRepository.findByActivationTokenHash(tokenHash);

        if (userOpt.isEmpty()) {
            return TokenVerificationResponse.invalid("Invalid or already used activation link");
        }

        User user = userOpt.get();
        if (user.getActivationTokenExpiresAt() == null ||
                user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return TokenVerificationResponse.invalid("Activation link has expired. Please request a new link.");
        }

        String employeeName = user.getUsername();
        if (user.getEmployeeId() != null && user.getCompany() != null) {
            Employee emp = employeeRepository
                    .findByCompanyIdAndEmployeeId(user.getCompany().getId(), user.getEmployeeId())
                    .orElse(null);
            if (emp != null && emp.getName() != null) {
                employeeName = emp.getName();
            }
        }

        String companyName = user.getCompany() != null ? user.getCompany().getName() : "Travora";
        return TokenVerificationResponse.valid(user.getUsername(), employeeName, companyName);
    }

    // =========================================================================
    // 8. ACTIVATE ACCOUNT & SET PASSWORD (PUBLIC)
    // =========================================================================

    @Transactional
    public void activateAccount(ActivateAccountRequest request) {
        String rawToken = request.getToken() != null ? request.getToken().trim() : "";
        String newPassword = request.getPassword() != null ? request.getPassword().trim() : "";

        if (rawToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation token is required");
        }
        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");
        }

        String tokenHash = hashToken(rawToken);
        User user = userRepository.findByActivationTokenHash(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or already used activation link"
                ));

        if (user.getActivationTokenExpiresAt() == null ||
                user.getActivationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Activation link has expired. Please request a new link from your administrator."
            );
        }

        // Set password and invalidate activation token
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setActivationTokenHash(null);
        user.setActivationTokenExpiresAt(null);
        user.setActive(true);

        userRepository.save(user);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void validateCallerCanManageUsers(User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Role role = currentUser.getRole();
        if (role != Role.TRAVEL_DESK &&
                role != Role.COMPANY_ADMIN &&
                role != Role.ADMIN &&
                role != Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Insufficient privileges to manage user accounts"
            );
        }
    }

    private void validateRoleAssignment(User currentUser, Role requestedRole) {
        if (requestedRole == Role.PLATFORM_ADMIN) {
            if (currentUser.getRole() != Role.PLATFORM_ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only platform administrators can create platform administrators"
                );
            }
        }

        if (currentUser.getRole() == Role.TRAVEL_DESK) {
            if (requestedRole == Role.COMPANY_ADMIN || requestedRole == Role.ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Travel desk personnel cannot assign administrative roles"
                );
            }
        }
    }

    private Employee resolveOrCreateEmployee(CreateUserRequest request, Company company) {
        String empId = request.getEmployeeId() != null ? request.getEmployeeId().trim() : "";

        if (!empId.isEmpty()) {
            // Check if employee already exists in current company
            Optional<Employee> existingInCompany = employeeRepository
                    .findByCompanyIdAndEmployeeId(company.getId(), empId);

            if (existingInCompany.isPresent()) {
                Employee employee = existingInCompany.get();
                // Verify not already associated with another user in the company
                Optional<User> linkedUser = userRepository.findByCompanyIdAndEmployeeId(company.getId(), empId);
                if (linkedUser.isPresent()) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "An account is already linked to employee ID '" + empId + "'"
                    );
                }
                // Update employee details with submitted values if present
                if (request.getName() != null && !request.getName().trim().isEmpty()) {
                    employee.setName(request.getName().trim());
                }
                if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                    employee.setEmail(request.getEmail().trim().toLowerCase());
                }
                if (request.getDepartment() != null) {
                    employee.setDepartment(request.getDepartment().trim());
                }
                if (request.getDesignation() != null) {
                    employee.setDesignation(request.getDesignation().trim());
                }
                return employeeRepository.save(employee);
            }

            // Create new Employee record with specified ID
            Employee newEmp = new Employee();
            newEmp.setCompany(company);
            newEmp.setEmployeeId(empId);
            newEmp.setName(request.getName().trim());
            newEmp.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
            newEmp.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
            newEmp.setDesignation(request.getDesignation() != null ? request.getDesignation().trim() : null);
            newEmp.setActive(true);
            return employeeRepository.save(newEmp);
        }

        // Auto-generate employeeId if not provided
        String companyCode = company != null && company.getCode() != null ? company.getCode() : "EMP";
        String generatedEmpId;
        int attempts = 0;
        do {
            generatedEmpId = companyCode + String.format("%04d", secureRandom.nextInt(10000));
            attempts++;
        } while (employeeRepository.existsByCompanyIdAndEmployeeId(company.getId(), generatedEmpId) && attempts < 10);

        Employee newEmp = new Employee();
        newEmp.setCompany(company);
        newEmp.setEmployeeId(generatedEmpId);
        newEmp.setName(request.getName().trim());
        newEmp.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        newEmp.setDepartment(request.getDepartment() != null ? request.getDepartment().trim() : null);
        newEmp.setDesignation(request.getDesignation() != null ? request.getDesignation().trim() : null);
        newEmp.setActive(true);
        return employeeRepository.save(newEmp);
    }

    private User findAndValidateTenantUser(Long id, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (currentUser.getRole() != Role.PLATFORM_ADMIN) {
            if (user.getCompany() == null || currentUser.getCompany() == null ||
                    !user.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
        }
        return user;
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String determineStatus(User user) {
        if (!user.isActive()) {
            return "INACTIVE";
        }
        if (user.getActivationTokenHash() != null) {
            return "PENDING_ACTIVATION";
        }
        return "ACTIVE";
    }

    private UserResponse mapToUserResponse(User user, Employee employee) {
        String name = employee != null ? employee.getName() : user.getUsername();
        String dept = employee != null ? employee.getDepartment() : null;
        String desig = employee != null ? employee.getDesignation() : null;
        String status = determineStatus(user);
        boolean hasPending = user.getActivationTokenHash() != null;

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                name,
                user.getRole(),
                user.isActive(),
                status,
                user.getEmployeeId(),
                dept,
                desig,
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getCode() : null,
                user.getCompany() != null ? user.getCompany().getName() : null,
                user.getCreatedAt(),
                hasPending
        );
    }

    private UserCreateResponse mapToUserCreateResponse(User user, Employee employee) {
        UserResponse base = mapToUserResponse(user, employee);
        UserCreateResponse response = new UserCreateResponse();
        response.setId(base.getId());
        response.setUsername(base.getUsername());
        response.setName(base.getName());
        response.setRole(base.getRole());
        response.setActive(base.isActive());
        response.setStatus(base.getStatus());
        response.setEmployeeId(base.getEmployeeId());
        response.setDepartment(base.getDepartment());
        response.setDesignation(base.getDesignation());
        response.setCompanyId(base.getCompanyId());
        response.setCompanyCode(base.getCompanyCode());
        response.setCompanyName(base.getCompanyName());
        response.setCreatedAt(base.getCreatedAt());
        response.setHasPendingActivation(base.isHasPendingActivation());
        return response;
    }

    private boolean matchesFilter(
            UserResponse user,
            String search,
            Role roleFilter,
            String statusFilter
    ) {
        if (roleFilter != null && user.getRole() != roleFilter) {
            return false;
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            if (!statusFilter.equalsIgnoreCase(user.getStatus())) {
                return false;
            }
        }

        if (search != null && !search.trim().isEmpty()) {
            String query = search.trim().toLowerCase();
            boolean matchName = user.getName() != null && user.getName().toLowerCase().contains(query);
            boolean matchEmail = user.getUsername() != null && user.getUsername().toLowerCase().contains(query);
            boolean matchEmpId = user.getEmployeeId() != null && user.getEmployeeId().toLowerCase().contains(query);
            boolean matchDept = user.getDepartment() != null && user.getDepartment().toLowerCase().contains(query);
            return matchName || matchEmail || matchEmpId || matchDept;
        }

        return true;
    }
}
