package com.traveldesk.backend.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.JwtService;
import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.company.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Employee Directory & Multi-Tenant Security Suite")
class EmployeeDirectorySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Company tenantA;
    private Company tenantB;

    private User adminA;
    private User travelDeskA;
    private User approverA;
    private User employeeA1;
    private User employeeA2;

    private User adminB;
    private User travelDeskB;
    private User employeeB1;

    private Employee empA1;
    private Employee empA2;
    private Employee empB1;

    private String tokenAdminA;
    private String tokenTravelDeskA;
    private String tokenApproverA;
    private String tokenEmployeeA1;
    private String tokenEmployeeA2;

    private String tokenAdminB;
    private String tokenTravelDeskB;
    private String tokenEmployeeB1;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        employeeRepository.deleteAll();
        companyRepository.deleteAll();

        // 1. Setup Tenant A
        tenantA = new Company();
        tenantA.setName("Acme Corporation");
        tenantA.setCode("ACME");
        tenantA.setActive(true);
        tenantA = companyRepository.save(tenantA);

        empA1 = createEmployeeRecord(tenantA, "EMP001", "Alice Smith", "Engineering", "alice@acme.com", true);
        empA2 = createEmployeeRecord(tenantA, "EMP002", "Bob Johnson", "Product", "bob@acme.com", false);

        adminA = createUserAccount(tenantA, "admin@acme.com", Role.COMPANY_ADMIN, "ADM01");
        travelDeskA = createUserAccount(tenantA, "desk@acme.com", Role.TRAVEL_DESK, "DSK01");
        approverA = createUserAccount(tenantA, "approver@acme.com", Role.APPROVER, "APP01");
        employeeA1 = createUserAccount(tenantA, "alice.user@acme.com", Role.EMPLOYEE, empA1.getEmployeeId());
        employeeA2 = createUserAccount(tenantA, "bob.user@acme.com", Role.EMPLOYEE, empA2.getEmployeeId());

        tokenAdminA = jwtService.generateToken(adminA);
        tokenTravelDeskA = jwtService.generateToken(travelDeskA);
        tokenApproverA = jwtService.generateToken(approverA);
        tokenEmployeeA1 = jwtService.generateToken(employeeA1);
        tokenEmployeeA2 = jwtService.generateToken(employeeA2);

        // 2. Setup Tenant B
        tenantB = new Company();
        tenantB.setName("Beta Global");
        tenantB.setCode("BETA");
        tenantB.setActive(true);
        tenantB = companyRepository.save(tenantB);

        // Note: empB1 intentionally uses the SAME identifier "EMP001" to verify company-scoped uniqueness
        empB1 = createEmployeeRecord(tenantB, "EMP001", "Charlie Davis", "Finance", "charlie@beta.com", true);

        adminB = createUserAccount(tenantB, "admin@beta.com", Role.COMPANY_ADMIN, "ADM_B01");
        travelDeskB = createUserAccount(tenantB, "desk@beta.com", Role.TRAVEL_DESK, "DSK_B01");
        employeeB1 = createUserAccount(tenantB, "charlie.user@beta.com", Role.EMPLOYEE, empB1.getEmployeeId());

        tokenAdminB = jwtService.generateToken(adminB);
        tokenTravelDeskB = jwtService.generateToken(travelDeskB);
        tokenEmployeeB1 = jwtService.generateToken(employeeB1);
    }

    private Employee createEmployeeRecord(Company company, String empId, String name, String dept, String email, boolean active) {
        Employee e = new Employee();
        e.setCompany(company);
        e.setEmployeeId(empId);
        e.setName(name);
        e.setDepartment(dept);
        e.setDesignation("Specialist");
        e.setLocation("Building 1");
        e.setEmail(email);
        e.setContactNumber("+91-9876543210");
        e.setActive(active);
        return employeeRepository.save(e);
    }

    private User createUserAccount(Company company, String username, Role role, String empId) {
        User u = new User();
        u.setCompany(company);
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("TestPassword123!"));
        u.setRole(role);
        u.setEmployeeId(empId);
        u.setActive(true);
        return userRepository.save(u);
    }

    // =========================================================================
    // 1. COMPANY-SCOPED UNIQUENESS
    // =========================================================================

    @Test
    @DisplayName("1. Two different companies can use the same employee identifier")
    void twoDifferentCompaniesCanUseSameEmployeeIdentifier() throws Exception {
        // Tenant A has EMP001 (Alice) and Tenant B has EMP001 (Charlie)
        mockMvc.perform(get("/api/employees/employee-id/EMP001")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is("EMP001")))
                .andExpect(jsonPath("$.name", is("Alice Smith")))
                .andExpect(jsonPath("$.companyCode", is("ACME")));

        mockMvc.perform(get("/api/employees/employee-id/EMP001")
                        .header("Authorization", "Bearer " + tokenAdminB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is("EMP001")))
                .andExpect(jsonPath("$.name", is("Charlie Davis")))
                .andExpect(jsonPath("$.companyCode", is("BETA")));
    }

    @Test
    @DisplayName("2. Duplicate employee identifiers within the same company are rejected")
    void duplicateEmployeeIdentifierWithinSameCompanyRejected() throws Exception {
        // Attempt to create another "EMP001" in Tenant A
        CreateEmployeeRequest req = new CreateEmployeeRequest();
        req.setEmployeeId("EMP001");
        req.setName("Alice Duplicate");
        req.setDepartment("Security");

        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // 2. PAGINATION, SEARCH, AND FILTERING
    // =========================================================================

    @Test
    @DisplayName("3. Pagination returns correct metadata and sliced content")
    void paginationReturnsCorrectMetadata() throws Exception {
        // Tenant A has 2 employees (Alice, Bob)
        mockMvc.perform(get("/api/employees?page=0&size=1&sort=name,asc")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(false)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Smith")));

        mockMvc.perform(get("/api/employees?page=1&size=1&sort=name,asc")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Johnson")));
    }

    @Test
    @DisplayName("4. Search and active-status filters work correctly")
    void searchAndActiveStatusFilterWorkCorrectly() throws Exception {
        // Search by name "alice"
        mockMvc.perform(get("/api/employees?search=alice")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Smith")));

        // Search by employeeId "EMP002"
        mockMvc.perform(get("/api/employees?search=EMP002")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Johnson")));

        // Filter active=true (only Alice is active)
        mockMvc.perform(get("/api/employees?active=true")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Alice Smith")));

        // Filter active=false (only Bob is inactive)
        mockMvc.perform(get("/api/employees?active=false")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Bob Johnson")));
    }

    // =========================================================================
    // 3. ROLE-BASED ACCESS & SELF-ACCESS
    // =========================================================================

    @Test
    @DisplayName("5. An employee cannot retrieve another employee's private record")
    void employeeCannotRetrieveAnotherEmployeeRecord() throws Exception {
        // employeeA1 (Alice, EMP001) can view own profile
        mockMvc.perform(get("/api/employees/" + empA1.getId())
                        .header("Authorization", "Bearer " + tokenEmployeeA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Smith")));

        // employeeA1 cannot view employeeA2 (Bob, EMP002) -> 403 Forbidden
        mockMvc.perform(get("/api/employees/" + empA2.getId())
                        .header("Authorization", "Bearer " + tokenEmployeeA1))
                .andExpect(status().isForbidden());

        // employeeA1 querying employee-id endpoint for Bob -> 403 Forbidden
        mockMvc.perform(get("/api/employees/employee-id/EMP002")
                        .header("Authorization", "Bearer " + tokenEmployeeA1))
                .andExpect(status().isForbidden());

        // employeeA1 calling list endpoint only receives own record
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + tokenEmployeeA1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].employeeId", is("EMP001")));
    }

    // =========================================================================
    // 4. CROSS-TENANT ISOLATION
    // =========================================================================

    @Test
    @DisplayName("6. Company A cannot retrieve Company B employee through list, search, or lookup")
    void companyACannotAccessCompanyBEmployee() throws Exception {
        // Admin A lists employees -> should only see Tenant A records
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].companyCode", everyItem(is("ACME"))))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Charlie Davis"))));

        // Admin A searches for "Charlie" -> returns 0 results
        mockMvc.perform(get("/api/employees?search=Charlie")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));

        // Admin A tries to get empB1 by ID -> 404 Not Found
        mockMvc.perform(get("/api/employees/" + empB1.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("7. Travel Desk users cannot cross company boundaries")
    void travelDeskCannotCrossCompanyBoundaries() throws Exception {
        // Travel Desk A can list Tenant A employees
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + tokenTravelDeskA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].companyCode", everyItem(is("ACME"))));

        // Travel Desk A accessing Tenant B employee ID -> 404 Not Found
        mockMvc.perform(get("/api/employees/" + empB1.getId())
                        .header("Authorization", "Bearer " + tokenTravelDeskA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8. Company administrators cannot cross company boundaries")
    void companyAdminCannotCrossCompanyBoundaries() throws Exception {
        // Admin A cannot update Tenant B employee
        UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest();
        updateReq.setName("Hacked Charlie");

        mockMvc.perform(put("/api/employees/" + empB1.getId())
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Admin A cannot delete Tenant B employee
        mockMvc.perform(delete("/api/employees/" + empB1.getId())
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Approver access follows company scope")
    void approverAccessFollowsCompanyScope() throws Exception {
        // Approver A querying directory sees Tenant A employees
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + tokenApproverA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].companyCode", everyItem(is("ACME"))));

        // Approver A querying Tenant B employee ID -> 404 Not Found
        mockMvc.perform(get("/api/employees/" + empB1.getId())
                        .header("Authorization", "Bearer " + tokenApproverA))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 5. INPUT VALIDATION & SAFE SORTING
    // =========================================================================

    @Test
    @DisplayName("10. Invalid page sizes and unsupported sort fields are handled safely")
    void invalidPaginationAndSortingHandledSafely() throws Exception {
        // Negative page index -> 400 Bad Request
        mockMvc.perform(get("/api/employees?page=-1")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());

        // Size > 100 -> 400 Bad Request
        mockMvc.perform(get("/api/employees?size=500")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());

        // Size < 1 -> 400 Bad Request
        mockMvc.perform(get("/api/employees?size=0")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());

        // Unsupported sort field -> 400 Bad Request
        mockMvc.perform(get("/api/employees?sort=nonExistentField,asc")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());

        // Malicious SQL injection in sort field -> 400 Bad Request
        mockMvc.perform(get("/api/employees?sort=id;DROP TABLE employees--,asc")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());

        // Invalid sort direction -> 400 Bad Request
        mockMvc.perform(get("/api/employees?sort=name,invalidDirection")
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 6. CRUD & SOFT DELETE
    // =========================================================================

    @Test
    @DisplayName("11. Existing employee-management behavior (create, update, soft-delete) remains functional")
    void employeeManagementCrudRemainsFunctional() throws Exception {
        // Create new employee
        CreateEmployeeRequest createReq = new CreateEmployeeRequest();
        createReq.setEmployeeId("EMP009");
        createReq.setName("Danielle Miller");
        createReq.setDepartment("Human Resources");
        createReq.setDesignation("HR Specialist");
        createReq.setLocation("Tower 3");
        createReq.setEmail("danielle@acme.com");
        createReq.setContactNumber("+91-9988776655");
        createReq.setActive(true);

        String createRes = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId", is("EMP009")))
                .andExpect(jsonPath("$.name", is("Danielle Miller")))
                .andExpect(jsonPath("$.email", is("danielle@acme.com")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn().getResponse().getContentAsString();

        Long newId = objectMapper.readTree(createRes).get("id").asLong();

        // Update employee
        UpdateEmployeeRequest updateReq = new UpdateEmployeeRequest();
        updateReq.setName("Danielle Miller-Smith");
        updateReq.setDesignation("Lead HR Specialist");

        mockMvc.perform(put("/api/employees/" + newId)
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Danielle Miller-Smith")))
                .andExpect(jsonPath("$.designation", is("Lead HR Specialist")));

        // Delete (soft-deactivate) employee
        mockMvc.perform(delete("/api/employees/" + newId)
                        .header("Authorization", "Bearer " + tokenAdminA))
                .andExpect(status().isNoContent());

        // Verify record is preserved but active is now false
        Employee deactivated = employeeRepository.findById(newId).orElseThrow();
        assertEquals(false, deactivated.isActive());
        assertEquals("Danielle Miller-Smith", deactivated.getName());
    }
}
