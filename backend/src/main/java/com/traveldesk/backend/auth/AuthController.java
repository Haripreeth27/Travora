package com.traveldesk.backend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
            user.setEmployeeId(request.getEmployeeId().trim());
        }

        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive()
        );
    }

    // =========================
    // ADMIN USER PROVISIONING
    // =========================

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
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

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(request.getRole() != null ? request.getRole() : Role.EMPLOYEE);
        user.setActive(true);
        if (request.getEmployeeId() != null && !request.getEmployeeId().trim().isEmpty()) {
            user.setEmployeeId(request.getEmployeeId().trim());
        }

        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive()
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
    // ADMIN ONLY
    // =========================

    @PutMapping("/users/{userId}/employee")
    @PreAuthorize("hasRole('ADMIN')")
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

        user.setEmployeeId(request.getEmployeeId());
        User saved = userRepository.save(user);

        return new UserSummaryResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getEmployeeId(),
                saved.isActive()
        );
    }

    // =========================
    // RESET PASSWORD
    // ADMIN ONLY
    // =========================

    @PutMapping("/reset-password/{username}")
    @PreAuthorize("hasRole('ADMIN')")
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

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        return "Password reset successfully";
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
