package com.traveldesk.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String createTestUserAndToken(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        return jwtService.generateToken(user);
    }

    @Test
    @DisplayName("Public registration cannot escalate privileges to ADMIN")
    void publicRegistrationCannotEscalateToAdmin() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setUsername("sneaky_user");
        request.setPassword("Secret123!");
        request.setRole(Role.ADMIN);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public registration defaults to EMPLOYEE role and excludes password in response")
    void publicRegistrationSucceedsAsEmployee() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setUsername("john_doe");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("john_doe")))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Unauthenticated request to travel requests returns 401 or 403")
    void unauthenticatedAccessIsBlocked() throws Exception {
        mockMvc.perform(get("/api/travel-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee cannot call admin deleteAll endpoint")
    void employeeCannotDeleteAllTravelRequests() throws Exception {
        String employeeToken = createTestUserAndToken("emp1", Role.EMPLOYEE);

        mockMvc.perform(delete("/api/travel-requests/all")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee cannot create employee records")
    void employeeCannotCreateEmployeeRecords() throws Exception {
        String employeeToken = createTestUserAndToken("emp2", Role.EMPLOYEE);

        String employeeJson = """
                {
                    "employeeId": "AGR9999",
                    "name": "Unauthorized User",
                    "department": "Engineering"
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can provision users with any role via /api/auth/users")
    void adminCanProvisionUserWithRole() throws Exception {
        String adminToken = createTestUserAndToken("admin_user", Role.ADMIN);

        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setUsername("desk_agent_1");
        request.setPassword("DeskPass123!");
        request.setRole(Role.TRAVEL_DESK);

        mockMvc.perform(post("/api/auth/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("desk_agent_1")))
                .andExpect(jsonPath("$.role", is("TRAVEL_DESK")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Authenticated user can retrieve /api/auth/me profile")
    void authenticatedUserCanRetrieveMeProfile() throws Exception {
        String token = createTestUserAndToken("profile_user", Role.EMPLOYEE);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("profile_user")))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
