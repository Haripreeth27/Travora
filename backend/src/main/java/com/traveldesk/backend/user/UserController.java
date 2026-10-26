package com.traveldesk.backend.user;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // =========================================================================
    // 1. CREATE USER
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreateResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User currentUser = getAuthenticatedUser();
        return userService.createUser(request, currentUser);
    }

    // =========================================================================
    // 2. LIST USERS
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    public List<UserResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String status
    ) {
        User currentUser = getAuthenticatedUser();
        return userService.listUsers(currentUser, search, role, status);
    }

    // =========================================================================
    // 3. GET USER BY ID
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    public UserResponse getUserById(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        return userService.getUserById(id, currentUser);
    }

    // =========================================================================
    // 4. UPDATE USER
    // =========================================================================

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        User currentUser = getAuthenticatedUser();
        return userService.updateUser(id, request, currentUser);
    }

    // =========================================================================
    // 5. UPDATE USER STATUS (ACTIVATE / DEACTIVATE)
    // =========================================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    public UserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        User currentUser = getAuthenticatedUser();
        return userService.updateUserStatus(id, request.getActive(), currentUser);
    }

    // =========================================================================
    // 6. RESEND ACTIVATION LINK
    // =========================================================================

    @PostMapping("/{id}/resend-activation")
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'COMPANY_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
    public UserCreateResponse resendActivation(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        return userService.resendActivation(id, currentUser);
    }
}
