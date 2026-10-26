package com.traveldesk.backend.auth;

import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.traveldesk.backend.user.ActivateAccountRequest;
import com.traveldesk.backend.user.TokenVerificationResponse;
import com.traveldesk.backend.user.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    // =========================
    // ACCOUNT ACTIVATION (PUBLIC)
    // =========================

    @GetMapping("/activate/verify")
    public TokenVerificationResponse verifyActivationToken(@RequestParam String token) {
        return userService.verifyActivationToken(token);
    }

    @PostMapping("/activate")
    public java.util.Map<String, String> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        userService.activateAccount(request);
        return java.util.Map.of("message", "Account successfully activated. You can now log in with your credentials.");
    }

    // =========================
    // CURRENT USER PROFILE
    // =========================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserSummaryResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getEmployeeId(),
                user.isActive(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getCode() : null,
                user.getCompany() != null ? user.getCompany().getName() : null
        );
    }

    // =========================
    // REGISTER (PUBLIC SELF-REGISTRATION: RESTRICTED TO EMPLOYEE)
    // =========================

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }
        if (password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        // Prevent unauthorized role elevation
        Role roleToAssign = Role.EMPLOYEE;
        if (request.getRole() != null && request.getRole() != Role.EMPLOYEE) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            if (!isAdmin) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only administrators can assign privileged roles"
                );
            }
            roleToAssign = request.getRole();
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(roleToAssign);
        user.setActive(true);
        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty()) {
            String empId = request.getEmployeeId().trim();
            user.setEmployeeId(empId);
            employeeRepository.findByEmployeeId(empId).ifPresent(emp -> {
                if (emp.getCompany() != null) {
                    user.setCompany(emp.getCompany());
                }
            });
        }

        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive(),
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCompany() != null ? saved.getCompany().getCode() : null,
                saved.getCompany() != null ? saved.getCompany().getName() : null
        );
    }

    // =========================
    // ADMIN USER PROVISIONING
    // =========================

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse createUser(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password cannot be empty");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User currentUser = getAuthenticatedUser();
        Role requestedRole = request.getRole() != null ? request.getRole() : Role.EMPLOYEE;

        // Prevent unauthorized privilege escalation
        if (requestedRole == Role.PLATFORM_ADMIN) {
            if (currentUser == null || currentUser.getRole() != Role.PLATFORM_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only platform administrators can create platform administrators");
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(requestedRole);
        user.setActive(true);

        if (currentUser != null && currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            user.setCompany(currentUser.getCompany());
        }

        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty()) {
            String empId = request.getEmployeeId().trim();
            user.setEmployeeId(empId);
            employeeRepository.findByEmployeeId(empId).ifPresent(emp -> {
                if (currentUser != null && currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && emp.getCompany() != null) {
                    if (!emp.getCompany().getId().equals(currentUser.getCompany().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign employee from another company");
                    }
                }
                if (user.getCompany() == null && emp.getCompany() != null) {
                    user.setCompany(emp.getCompany());
                }
            });
        }

        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive(),
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCompany() != null ? saved.getCompany().getCode() : null,
                saved.getCompany() != null ? saved.getCompany().getName() : null
        );
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid username or password"
                        )
                );

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User account is inactive"
            );
        }

        if (user.getCompany() != null && !user.getCompany().isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Company account is inactive"
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getEmployeeId(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                user.getCompany() != null ? user.getCompany().getCode() : null,
                user.getCompany() != null ? user.getCompany().getName() : null
        );
    }

    // =========================
    // ASSIGN EMPLOYEE TO USER
    // =========================

    @PutMapping("/users/{userId}/employee")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public UserSummaryResponse assignEmployee(
            @PathVariable Long userId,
            @RequestBody EmployeeAssignmentRequest request
    ) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        User currentUser = getAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            if (user.getCompany() != null && !user.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cannot modify users of another company"
                );
            }
        }

        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty()) {
            String empId = request.getEmployeeId().trim();
            user.setEmployeeId(empId);
            employeeRepository.findByEmployeeId(empId).ifPresent(emp -> {
                if (currentUser != null && currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && emp.getCompany() != null) {
                    if (!emp.getCompany().getId().equals(currentUser.getCompany().getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign employee from another company");
                    }
                }
                if (user.getCompany() == null && emp.getCompany() != null) {
                    user.setCompany(emp.getCompany());
                }
            });
        } else {
            user.setEmployeeId(null);
        }

        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive(),
                saved.getCompany() != null ? saved.getCompany().getId() : null,
                saved.getCompany() != null ? saved.getCompany().getCode() : null,
                saved.getCompany() != null ? saved.getCompany().getName() : null
        );
    }

    // =========================
    // RESET PASSWORD
    // =========================

    @PutMapping("/reset-password/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public String resetPassword(
            @PathVariable String username,
            @RequestBody PasswordResetRequest request
    ) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        User currentUser = getAuthenticatedUser();
        if (currentUser != null && currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null) {
            if (user.getCompany() != null && !user.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Cannot reset password for users of another company"
                );
            }
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        return "Password reset successfully";
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    // =========================
    // DTOS & REQUEST OBJECTS
    // =========================

    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        private Role role;
        private String employeeId;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class EmployeeAssignmentRequest {
        private String employeeId;

        public String getEmployeeId() {
            return employeeId;
        }

        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
    }

    public static class PasswordResetRequest {
        private String newPassword;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class UserSummaryResponse {
        private Long id;
        private String username;
        private Role role;
        private String employeeId;
        private boolean active;
        private Long companyId;
        private String companyCode;
        private String companyName;

        public UserSummaryResponse(Long id, String username, Role role, String employeeId, boolean active) {
            this(id, username, role, employeeId, active, null, null, null);
        }

        public UserSummaryResponse(Long id, String username, Role role, String employeeId, boolean active,
                                   Long companyId, String companyCode, String companyName) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.employeeId = employeeId;
            this.active = active;
            this.companyId = companyId;
            this.companyCode = companyCode;
            this.companyName = companyName;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public Role getRole() {
            return role;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public boolean isActive() {
            return active;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public String getCompanyName() {
            return companyName;
        }
    }

    public static class LoginResponse {
        private String token;
        private Long id;
        private String username;
        private Role role;
        private String employeeId;
        private Long companyId;
        private String companyCode;
        private String companyName;

        public LoginResponse(
                String token,
                Long id,
                String username,
                Role role,
                String employeeId
        ) {
            this(token, id, username, role, employeeId, null, null, null);
        }

        public LoginResponse(
                String token,
                Long id,
                String username,
                Role role,
                String employeeId,
                Long companyId,
                String companyCode,
                String companyName
        ) {
            this.token = token;
            this.id = id;
            this.username = username;
            this.role = role;
            this.employeeId = employeeId;
            this.companyId = companyId;
            this.companyCode = companyCode;
            this.companyName = companyName;
        }

        public String getToken() {
            return token;
        }

        public Long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public Role getRole() {
            return role;
        }

        public String getEmployeeId() {
            return employeeId;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public String getCompanyName() {
            return companyName;
        }
    }
}
