package com.traveldesk.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.*;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.company.CompanyRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import com.traveldesk.backend.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Management & Account Creation Security Suite")
class UserManagementSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Company tenantA;
    private Company tenantB;

    private User adminA;
    private User travelDeskA;
    private User employeeA;

    private User adminB;
    private User travelDeskB;
    private User employeeB;

    private String tokenAdminA;
    private String tokenTravelDeskA;
    private String tokenEmployeeA;

    private String tokenAdminB;
    private String tokenTravelDeskB;
    private String tokenEmployeeB;

    @BeforeEach
    void setupData() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        companyRepository.deleteAll();

        // Setup Tenant A
        tenantA = new Company();
        tenantA.setName("Acme Corporation");
        tenantA.setCode("ACME");
        tenantA.setActive(true);
        tenantA = companyRepository.save(tenantA);

        adminA = createUser("admin_a@acme.com", Role.COMPANY_ADMIN, tenantA, "EMP_A01");
        travelDeskA = createUser("desk_a@acme.com", Role.TRAVEL_DESK, tenantA, "EMP_A02");
        employeeA = createUser("emp_a@acme.com", Role.EMPLOYEE, tenantA, "EMP_A03");

        tokenAdminA = jwtService.generateToken(adminA);
        tokenTravelDeskA = jwtService.generateToken(travelDeskA);
        tokenEmployeeA = jwtService.generateToken(employeeA);

        // Setup Tenant B
        tenantB = new Company();
        tenantB.setName("Beta Industries");
        tenantB.setCode("BETA");
        tenantB.setActive(true);
        tenantB = companyRepository.save(tenantB);

        adminB = createUser("admin_b@beta.com", Role.COMPANY_ADMIN, tenantB, "EMP_B01");
        travelDeskB = createUser("desk_b@beta.com", Role.TRAVEL_DESK, tenantB, "EMP_B02");
        employeeB = createUser("emp_b@beta.com", Role.EMPLOYEE, tenantB, "EMP_B03");

        tokenAdminB = jwtService.generateToken(adminB);
        tokenTravelDeskB = jwtService.generateToken(travelDeskB);
        tokenEmployeeB = jwtService.generateToken(employeeB);
    }

    private User createUser(String email, Role role, Company company, String empId) {
        Employee emp = new Employee();
        emp.setCompany(company);
        emp.setEmployeeId(empId);
        emp.setName("User " + empId);
        emp.setDepartment("Engineering");
        emp.setDesignation("Engineer");
        employeeRepository.save(emp);

        User u = new User();
        u.setUsername(email);
        u.setPassword(passwordEncoder.encode("Password123!"));
        u.setRole(role);
        u.setCompany(company);
        u.setEmployeeId(empId);
        u.setActive(true);
        return userRepository.save(u);
    }

    // =========================================================================
    // 1. ACCOUNT CREATION & ROLE PERMISSIONS
    // =========================================================================

    @Test
    @DisplayName("Travel Desk can create employee account successfully with activation token")
    void travelDeskCanCreateEmployeeAccountSuccessfully() throws Exception {
        String payload = """
                {
                    "name": "Rajesh Sharma",
                    "email": "rajesh.sharma@acme.com",
                    "department": "Logistics",
                    "designation": "Supply Lead",
                    "role": "EMPLOYEE"
                }
                """;

        String res = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("rajesh.sharma@acme.com")))
                .andExpect(jsonPath("$.name", is("Rajesh Sharma")))
                .andExpect(jsonPath("$.role", is("EMPLOYEE")))
                .andExpect(jsonPath("$.status", is("PENDING_ACTIVATION")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.hasPendingActivation", is(true)))
                .andExpect(jsonPath("$.activationToken", notNullValue()))
                .andExpect(jsonPath("$.activationUrl", containsString("/activate?token=")))
                .andExpect(jsonPath("$.employeeId", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        // Verify linked Employee was created in database
        Optional<User> savedUser = userRepository.findByUsername("rajesh.sharma@acme.com");
        assertTrue(savedUser.isPresent());
        assertEquals(tenantA.getId(), savedUser.get().getCompany().getId());

        Optional<Employee> savedEmp = employeeRepository.findByEmployeeId(savedUser.get().getEmployeeId());
        assertTrue(savedEmp.isPresent());
        assertEquals("Rajesh Sharma", savedEmp.get().getName());
        assertEquals(tenantA.getId(), savedEmp.get().getCompany().getId());
    }

    @Test
    @DisplayName("Travel Desk can create Approver account")
    void travelDeskCanCreateApproverAccount() throws Exception {
        String payload = """
                {
                    "name": "Sonia Verma",
                    "email": "sonia.verma@acme.com",
                    "department": "Finance",
                    "designation": "Manager",
                    "role": "APPROVER"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("APPROVER")));
    }

    @Test
    @DisplayName("Travel Desk cannot create administrative roles or platform admin")
    void travelDeskCannotCreateAdminRoles() throws Exception {
        // Attempt COMPANY_ADMIN -> 403
        String payload1 = """
                {
                    "name": "Hacked Admin",
                    "email": "hacked.admin@acme.com",
                    "role": "COMPANY_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isForbidden());

        // Attempt PLATFORM_ADMIN -> 403
        String payload2 = """
                {
                    "name": "Super Admin Attempt",
                    "email": "super.attempt@acme.com",
                    "role": "PLATFORM_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Company Admin cannot create Platform Admin")
    void companyAdminCannotCreatePlatformAdmin() throws Exception {
        String payload = """
                {
                    "name": "Platform Attempt",
                    "email": "platform.attempt@acme.com",
                    "role": "PLATFORM_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. VALIDATION & DUPLICATE HANDLING
    // =========================================================================

    @Test
    @DisplayName("Duplicate email address returns 409 Conflict")
    void duplicateEmailReturnsConflict() throws Exception {
        String payload = """
                {
                    "name": "Duplicate User",
                    "email": "emp_a@acme.com",
                    "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Reuses existing unlinked Employee record without creating duplicate")
    void reusesExistingEmployeeRecordWithoutDuplicate() throws Exception {
        // Create an existing standalone Employee in Tenant A
        Employee standalone = new Employee();
        standalone.setCompany(tenantA);
        standalone.setEmployeeId("ACME999");
        standalone.setName("Pre Existing Standalone");
        standalone.setDepartment("Operations");
        employeeRepository.save(standalone);

        long employeeCountBefore = employeeRepository.count();

        String payload = """
                {
                    "name": "Pre Existing Standalone",
                    "email": "pre.existing@acme.com",
                    "employeeId": "ACME999",
                    "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId", is("ACME999")));

        // Employee count must NOT increase
        assertEquals(employeeCountBefore, employeeRepository.count());
    }

    @Test
    @DisplayName("Attempting to link already assigned employee ID returns 409 Conflict")
    void alreadyAssignedEmployeeIdReturnsConflict() throws Exception {
        // employeeA already has EMP_A03
        String payload = """
                {
                    "name": "Duplicate Emp Id",
                    "email": "new.user@acme.com",
                    "employeeId": "EMP_A03",
                    "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 3. TENANT ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Tenant A cannot list Tenant B users")
    void tenantACannotListTenantBUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].username", everyItem(containsString("@acme.com"))))
                .andExpect(jsonPath("$[*].username", not(hasItem("emp_b@beta.com"))));
    }

    @Test
    @DisplayName("Tenant A cannot retrieve Tenant B user details")
    void tenantACannotGetUserBDetails() throws Exception {
        mockMvc.perform(get("/api/users/" + employeeB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant A cannot update Tenant B user")
    void tenantACannotUpdateTenantBUser() throws Exception {
        String payload = """
                {
                    "name": "Hacked Beta User"
                }
                """;

        mockMvc.perform(patch("/api/users/" + employeeB.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant A cannot deactivate Tenant B user")
    void tenantACannotDeactivateTenantBUser() throws Exception {
        String payload = """
                {
                    "active": false
                }
                """;

        mockMvc.perform(patch("/api/users/" + employeeB.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. UNAUTHORIZED ROLE ACCESS
    // =========================================================================

    @Test
    @DisplayName("Standard Employee cannot access user management endpoints")
    void employeeCannotAccessUserManagement() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + tokenEmployeeA))
                .andExpect(status().isForbidden());

        String payload = """
                {
                    "name": "Unauthorized Attempt",
                    "email": "unauth@acme.com",
                    "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenEmployeeA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 5. DEACTIVATION & REACTIVATION LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Deactivated user has their account revoked and cannot authenticate")
    void deactivationBlocksUserAccess() throws Exception {
        // 1. Deactivate employeeA
        mockMvc.perform(patch("/api/users/" + employeeA.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        // 2. Verified that deactivated user cannot make requests with their JWT
        mockMvc.perform(get("/api/travel-requests")
                        .header("Authorization", "Bearer " + tokenEmployeeA))
                .andExpect(status().isForbidden());

        // 2b. Also verify login fails with 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"emp_a@acme.com\", \"password\": \"Password123!\"}"))
                .andExpect(status().isUnauthorized());

        // 3. Reactivate employeeA
        mockMvc.perform(patch("/api/users/" + employeeA.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        // 4. Employee can access again
        mockMvc.perform(get("/api/travel-requests")
                        .header("Authorization", "Bearer " + tokenEmployeeA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Administrator cannot deactivate their own account")
    void adminCannotDeactivateSelf() throws Exception {
        mockMvc.perform(patch("/api/users/" + adminA.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 6. ACTIVATION & FIRST LOGIN WORKFLOW
    // =========================================================================

    @Test
    @DisplayName("Complete activation workflow: token verification, password setup, and login")
    void fullActivationWorkflow() throws Exception {
        // 1. Travel Desk creates account
        String createPayload = """
                {
                    "name": "Kavita Rao",
                    "email": "kavita.rao@acme.com",
                    "department": "Design",
                    "designation": "Lead Designer",
                    "role": "EMPLOYEE"
                }
                """;

        String res = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String rawToken = objectMapper.readTree(res).get("activationToken").asText();
        assertNotNull(rawToken);

        // 2. Employee verifies token publicly
        mockMvc.perform(get("/api/auth/activate/verify")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is("kavita.rao@acme.com")))
                .andExpect(jsonPath("$.name", is("Kavita Rao")));

        // 3. Employee sets their password
        String activatePayload = """
                {
                    "token": "%s",
                    "password": "MyNewSecurePassword99!"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("successfully activated")));

        // 4. Employee logs in with their newly set password
        String loginPayload = """
                {
                    "username": "kavita.rao@acme.com",
                    "password": "MyNewSecurePassword99!"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("kavita.rao@acme.com")));

        // 5. Token cannot be reused
        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Invalid or bogus activation token is rejected")
    void invalidActivationTokenRejected() throws Exception {
        mockMvc.perform(get("/api/auth/activate/verify")
                        .param("token", "totally-invalid-token-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        String activatePayload = """
                {
                    "token": "bogus-token-xyz",
                    "password": "ValidPassword123!"
                }
                """;

        mockMvc.perform(post("/api/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activatePayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Resend activation generates fresh token and invalidates old one")
    void resendActivationGeneratesFreshToken() throws Exception {
        // 1. Create account
        String createPayload = """
                {
                    "name": "Manoj Kumar",
                    "email": "manoj.kumar@acme.com",
                    "role": "EMPLOYEE"
                }
                """;

        String res1 = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + tokenTravelDeskA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long userId = objectMapper.readTree(res1).get("id").asLong();
        String token1 = objectMapper.readTree(res1).get("activationToken").asText();

        // 2. Resend activation
        String res2 = mockMvc.perform(post("/api/users/" + userId + "/resend-activation")
                        .header("Authorization", "Bearer " + tokenTravelDeskA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activationToken", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        String token2 = objectMapper.readTree(res2).get("activationToken").asText();
        assertNotEquals(token1, token2);

        // 3. Old token should now be invalid
        mockMvc.perform(get("/api/auth/activate/verify").param("token", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)));

        // 4. New token should be valid
        mockMvc.perform(get("/api/auth/activate/verify").param("token", token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));
    }
}
