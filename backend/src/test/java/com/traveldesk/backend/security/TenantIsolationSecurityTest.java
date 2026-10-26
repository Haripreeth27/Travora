package com.traveldesk.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.AuthController;
import com.traveldesk.backend.auth.JwtService;
import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.booking.Booking;
import com.traveldesk.backend.booking.BookingRepository;
import com.traveldesk.backend.booking.BookingType;
import com.traveldesk.backend.bulkbooking.BulkBooking;
import com.traveldesk.backend.bulkbooking.BulkBookingRepository;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.company.CompanyRegistrationRequest;
import com.traveldesk.backend.company.CompanyRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import com.traveldesk.backend.expense.Expense;
import com.traveldesk.backend.expense.ExpenseCategory;
import com.traveldesk.backend.expense.ExpenseRepository;
import com.traveldesk.backend.expense.ExpenseRequest;
import com.traveldesk.backend.expense.ExpenseStatus;
import com.traveldesk.backend.travelrequest.ApprovalDecision;
import com.traveldesk.backend.travelrequest.TravelRequest;
import com.traveldesk.backend.travelrequest.TravelRequestRepository;
import com.traveldesk.backend.travelrequest.TravelRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TenantIsolationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TravelRequestRepository travelRequestRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BulkBookingRepository bulkBookingRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private com.traveldesk.backend.notification.NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Company tenantA;
    private Company tenantB;

    private User adminA;
    private User approverA;
    private User travelDeskA;
    private User employeeUserA;
    private Employee employeeA;

    private User adminB;
    private User approverB;
    private User travelDeskB;
    private User employeeUserB;
    private Employee employeeB;

    private String tokenAdminA;
    private String tokenApproverA;
    private String tokenTravelDeskA;
    private String tokenEmployeeA;

    private String tokenAdminB;
    private String tokenApproverB;
    private String tokenTravelDeskB;
    private String tokenEmployeeB;

    @BeforeEach
    void setupTenantsAndUsers() {
        // Clean up prior test data
        notificationRepository.deleteAll();
        expenseRepository.deleteAll();
        bookingRepository.deleteAll();
        travelRequestRepository.deleteAll();
        bulkBookingRepository.deleteAll();

        // Setup Tenant Alpha
        tenantA = companyRepository.findByCode("ALPHA_CORP").orElseGet(() -> {
            Company c = new Company("Alpha Corp", "ALPHA_CORP", "alphacorp.com");
            c.setActive(true);
            return companyRepository.save(c);
        });

        // Setup Tenant Beta
        tenantB = companyRepository.findByCode("BETA_CORP").orElseGet(() -> {
            Company c = new Company("Beta Corp", "BETA_CORP", "betacorp.com");
            c.setActive(true);
            return companyRepository.save(c);
        });

        employeeA = createOrGetEmployee("ALPHA_EMP_01", "Alice Alpha", tenantA);
        employeeB = createOrGetEmployee("BETA_EMP_01", "Bob Beta", tenantB);

        adminA = createOrGetUser("admin_alpha", Role.COMPANY_ADMIN, tenantA, null);
        approverA = createOrGetUser("approver_alpha", Role.APPROVER, tenantA, null);
        travelDeskA = createOrGetUser("desk_alpha", Role.TRAVEL_DESK, tenantA, null);
        employeeUserA = createOrGetUser("alice_alpha", Role.EMPLOYEE, tenantA, employeeA.getEmployeeId());

        adminB = createOrGetUser("admin_beta", Role.COMPANY_ADMIN, tenantB, null);
        approverB = createOrGetUser("approver_beta", Role.APPROVER, tenantB, null);
        travelDeskB = createOrGetUser("desk_beta", Role.TRAVEL_DESK, tenantB, null);
        employeeUserB = createOrGetUser("bob_beta", Role.EMPLOYEE, tenantB, employeeB.getEmployeeId());

        tokenAdminA = jwtService.generateToken(adminA);
        tokenApproverA = jwtService.generateToken(approverA);
        tokenTravelDeskA = jwtService.generateToken(travelDeskA);
        tokenEmployeeA = jwtService.generateToken(employeeUserA);

        tokenAdminB = jwtService.generateToken(adminB);
        tokenApproverB = jwtService.generateToken(approverB);
        tokenTravelDeskB = jwtService.generateToken(travelDeskB);
        tokenEmployeeB = jwtService.generateToken(employeeUserB);
    }

    private Employee createOrGetEmployee(String code, String name, Company company) {
        return employeeRepository.findByEmployeeId(code).orElseGet(() -> {
            Employee e = new Employee();
            e.setEmployeeId(code);
            e.setName(name);
            e.setDepartment("Engineering");
            e.setDesignation("Engineer");
            e.setLocation("Mumbai");
            e.setCompany(company);
            return employeeRepository.save(e);
        });
    }

    private User createOrGetUser(String username, Role role, Company company, String employeeId) {
        User u = userRepository.findByUsername(username).orElseGet(User::new);
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("SecretPass123!"));
        u.setRole(role);
        u.setActive(true);
        u.setCompany(company);
        u.setEmployeeId(employeeId);
        return userRepository.save(u);
    }

    // =========================================================================
    // SECTION A: TENANT ISOLATION
    // =========================================================================

    @Nested
    @DisplayName("Tenant Isolation Verification")
    class TenantIsolationTests {

        @Test
        @DisplayName("Company A cannot read Company B's expenses")
        void companyACannotReadCompanyBExpenses() throws Exception {
            Expense expB = new Expense();
            expB.setCompany(tenantB);
            expB.setEmployee(employeeB);
            expB.setCategory(ExpenseCategory.ACCOMMODATION);
            expB.setAmount(new BigDecimal("4500.00"));
            expB.setCurrency("INR");
            expB.setExpenseDate(LocalDate.now());
            expB.setMerchant("Beta Luxury Hotel");
            expB.setStatus(ExpenseStatus.SUBMITTED);
            expB = expenseRepository.save(expB);

            // Company A Approver attempts to read Company B expense by ID -> 404
            mockMvc.perform(get("/api/expenses/" + expB.getId())
                            .header("Authorization", "Bearer " + tokenApproverA))
                    .andExpect(status().isNotFound());

            // Company A Employee attempts to read Company B expense -> 404
            mockMvc.perform(get("/api/expenses/" + expB.getId())
                            .header("Authorization", "Bearer " + tokenEmployeeA))
                    .andExpect(status().isNotFound());

            // Company A Admin list query should NOT include Company B expense
            mockMvc.perform(get("/api/expenses")
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", not(hasItem(expB.getId().intValue()))));
        }

        @Test
        @DisplayName("Company A cannot approve or reject Company B's travel requests")
        void companyACannotApproveCompanyBTravelRequest() throws Exception {
            TravelRequest trB = new TravelRequest();
            trB.setEmployee(employeeB);
            trB.setCompany(tenantB);
            trB.setTripType("ONE_WAY");
            trB.setFromLocation("Delhi");
            trB.setToLocation("Bengaluru");
            trB.setTravelDate(LocalDate.now().plusDays(2));
            trB.setReturnDate(LocalDate.now().plusDays(5));
            trB.setProjectName("Beta Project");
            trB.setReason("Client Onboarding");
            trB.setStatus(TravelRequestStatus.PENDING);
            trB = travelRequestRepository.save(trB);

            ApprovalDecision decision = new ApprovalDecision();
            decision.setComment("Approved by malicious outsider");

            // Company A approver calls approve on Company B request -> 404
            mockMvc.perform(patch("/api/travel-requests/" + trB.getId() + "/approve")
                            .header("Authorization", "Bearer " + tokenApproverA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decision)))
                    .andExpect(status().isNotFound());

            // Company A approver calls reject on Company B request -> 404
            mockMvc.perform(patch("/api/travel-requests/" + trB.getId() + "/reject")
                            .header("Authorization", "Bearer " + tokenApproverA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decision)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Company A cannot modify or delete Company B's bookings")
        void companyACannotModifyCompanyBBooking() throws Exception {
            TravelRequest trB = new TravelRequest();
            trB.setEmployee(employeeB);
            trB.setCompany(tenantB);
            trB.setTripType("ONE_WAY");
            trB.setFromLocation("Delhi");
            trB.setToLocation("Bengaluru");
            trB.setTravelDate(LocalDate.now().plusDays(1));
            trB.setProjectName("Beta Project");
            trB.setStatus(TravelRequestStatus.APPROVED);
            trB = travelRequestRepository.save(trB);

            Booking bB = new Booking();
            bB.setCompany(tenantB);
            bB.setTravelRequest(trB);
            bB.setBookingType(BookingType.FLIGHT);
            bB.setBookingReference("BETA-FL-100");
            bB.setProvider("IndiGo");
            bB.setCost(new BigDecimal("7500.00"));
            bB.setSavings(new BigDecimal("500.00"));
            bB = bookingRepository.save(bB);

            // Company A Travel Desk tries to cancel Company B booking -> 404
            mockMvc.perform(put("/api/bookings/" + bB.getId() + "/cancel")
                            .header("Authorization", "Bearer " + tokenTravelDeskA)
                            .param("reason", "Malicious cancellation")
                            .param("cancellationCharge", "0"))
                    .andExpect(status().isNotFound());

            // Company A Admin tries to delete Company B booking -> 404
            mockMvc.perform(delete("/api/bookings/" + bB.getId())
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Company A cannot access Company B's employee records")
        void companyACannotAccessCompanyBEmployee() throws Exception {
            // Company A Admin attempts to get Company B employee by ID -> 404
            mockMvc.perform(get("/api/employees/" + employeeB.getId())
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isNotFound());

            // Company A Admin attempts to get Company B employee by code -> 404
            mockMvc.perform(get("/api/employees/employee-id/" + employeeB.getEmployeeId())
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isNotFound());

            // Company A list query must only return Company A employees
            mockMvc.perform(get("/api/employees")
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].employeeId", not(hasItem("BETA_EMP_01"))));
        }

        @Test
        @DisplayName("Employee cannot access another employee's private record within company")
        void employeeCannotAccessOtherEmployeePrivateRecord() throws Exception {
            Employee colleagueA = createOrGetEmployee("ALPHA_EMP_02", "Charlie Colleague", tenantA);

            // Alice tries to get Charlie's employee profile by ID -> 403 Forbidden
            mockMvc.perform(get("/api/employees/" + colleagueA.getId())
                            .header("Authorization", "Bearer " + tokenEmployeeA))
                    .andExpect(status().isForbidden());

            // Alice can access her own profile
            mockMvc.perform(get("/api/employees/" + employeeA.getId())
                            .header("Authorization", "Bearer " + tokenEmployeeA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId", is("ALPHA_EMP_01")));
        }

        @Test
        @DisplayName("Dashboard summary strictly calculates metrics from authorized company only")
        void dashboardMetricsAreStrictlyCompanyScoped() throws Exception {
            // Setup requests for Tenant A
            TravelRequest reqA = new TravelRequest();
            reqA.setEmployee(employeeA);
            reqA.setCompany(tenantA);
            reqA.setTripType("ONE_WAY");
            reqA.setFromLocation("Mumbai");
            reqA.setToLocation("Goa");
            reqA.setTravelDate(LocalDate.now().plusDays(1));
            reqA.setProjectName("Alpha Goa Project");
            reqA.setStatus(TravelRequestStatus.PENDING);
            travelRequestRepository.save(reqA);

            // Setup multiple requests for Tenant B
            TravelRequest reqB1 = new TravelRequest();
            reqB1.setEmployee(employeeB);
            reqB1.setCompany(tenantB);
            reqB1.setTripType("ONE_WAY");
            reqB1.setFromLocation("Delhi");
            reqB1.setToLocation("Jaipur");
            reqB1.setTravelDate(LocalDate.now().plusDays(1));
            reqB1.setProjectName("Beta Jaipur Project");
            reqB1.setStatus(TravelRequestStatus.PENDING);
            travelRequestRepository.save(reqB1);

            TravelRequest reqB2 = new TravelRequest();
            reqB2.setEmployee(employeeB);
            reqB2.setCompany(tenantB);
            reqB2.setTripType("ONE_WAY");
            reqB2.setFromLocation("Delhi");
            reqB2.setToLocation("Kolkata");
            reqB2.setTravelDate(LocalDate.now().plusDays(1));
            reqB2.setProjectName("Beta Kolkata Project");
            reqB2.setStatus(TravelRequestStatus.PENDING);
            travelRequestRepository.save(reqB2);

            // Admin A calls dashboard summary -> must only see Tenant A's 1 pending request
            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTravelRequests", is(1)))
                    .andExpect(jsonPath("$.pendingRequests", is(1)));

            // Admin B calls dashboard summary -> must only see Tenant B's 2 pending requests
            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Authorization", "Bearer " + tokenAdminB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTravelRequests", is(2)))
                    .andExpect(jsonPath("$.pendingRequests", is(2)));
        }

        @Test
        @DisplayName("Company A cannot access or modify Company B's bulk bookings")
        void bulkBookingIsolationEnforced() throws Exception {
            BulkBooking bulkB = new BulkBooking();
            bulkB.setTitle("Beta Annual Strategy Offsite");
            bulkB.setDestination("Goa");
            bulkB.setFromLocation("Delhi");
            bulkB.setTravelDates("Nov 10-14");
            bulkB.setTravelMode("Flight");
            bulkB.setGroupReference("BETA-GRP-01");
            bulkB.setHeadcount(25);
            bulkB.setTotalCost(new BigDecimal("250000.00"));
            bulkB.setCostPerPerson(new BigDecimal("10000.00"));
            bulkB.setCompany(tenantB);
            bulkB = bulkBookingRepository.save(bulkB);

            // Company A Travel Desk tries to get Company B's bulk booking -> 404
            mockMvc.perform(get("/api/bulk-bookings/" + bulkB.getId())
                            .header("Authorization", "Bearer " + tokenTravelDeskA))
                    .andExpect(status().isNotFound());

            // Company A Admin list query does not contain Company B's bulk booking
            mockMvc.perform(get("/api/bulk-bookings")
                            .header("Authorization", "Bearer " + tokenAdminA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", not(hasItem(bulkB.getId().intValue()))));
        }

        @Test
        @DisplayName("Cross-company entity linking is strictly rejected")
        void crossCompanyEntityLinkingRejected() throws Exception {
            TravelRequest trB = new TravelRequest();
            trB.setEmployee(employeeB);
            trB.setCompany(tenantB);
            trB.setTripType("ONE_WAY");
            trB.setFromLocation("Kolkata");
            trB.setToLocation("Hyderabad");
            trB.setTravelDate(LocalDate.now().plusDays(1));
            trB.setProjectName("Beta Project");
            trB.setStatus(TravelRequestStatus.APPROVED);
            trB = travelRequestRepository.save(trB);

            TravelRequest trA = new TravelRequest();
            trA.setEmployee(employeeA);
            trA.setCompany(tenantA);
            trA.setTripType("ONE_WAY");
            trA.setFromLocation("Mumbai");
            trA.setToLocation("Pune");
            trA.setTravelDate(LocalDate.now().plusDays(1));
            trA.setProjectName("Alpha Project");
            trA.setStatus(TravelRequestStatus.APPROVED);
            trA = travelRequestRepository.save(trA);

            Booking bookingA = new Booking();
            bookingA.setCompany(tenantA);
            bookingA.setTravelRequest(trA);
            bookingA.setBookingType(BookingType.CAB);
            bookingA.setBookingReference("ALPHA-CAB-01");
            bookingA.setProvider("Ola Corporate");
            bookingA.setCost(new BigDecimal("1200.00"));
            bookingA = bookingRepository.save(bookingA);

            // Malicious payload: Company A tries to update its booking to link Tenant B's travel request
            String updatePayload = """
                    {
                        "travelRequest": { "id": %d },
                        "bookingReference": "ALPHA-CAB-HIJACK"
                    }
                    """.formatted(trB.getId());

            mockMvc.perform(put("/api/bookings/" + bookingA.getId())
                            .header("Authorization", "Bearer " + tokenTravelDeskA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().is4xxClientError());
        }
    }

    // =========================================================================
    // SECTION B: AUTHENTICATION, AUTHORIZATION & PRIVILEGE ESCALATION
    // =========================================================================

    @Nested
    @DisplayName("Authentication & Authorization Security")
    class AuthSecurityTests {

        @Test
        @DisplayName("Public registration cannot escalate to PLATFORM_ADMIN or COMPANY_ADMIN")
        void publicRegistrationCannotEscalatePrivileges() throws Exception {
            AuthController.RegisterRequest req1 = new AuthController.RegisterRequest();
            req1.setUsername("hacker_platform_admin");
            req1.setPassword("Pass1234!");
            req1.setRole(Role.PLATFORM_ADMIN);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req1)))
                    .andExpect(status().isForbidden());

            AuthController.RegisterRequest req2 = new AuthController.RegisterRequest();
            req2.setUsername("hacker_company_admin");
            req2.setPassword("Pass1234!");
            req2.setRole(Role.COMPANY_ADMIN);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company administrator cannot provision a PLATFORM_ADMIN")
        void companyAdminCannotProvisionPlatformAdmin() throws Exception {
            AuthController.RegisterRequest req = new AuthController.RegisterRequest();
            req.setUsername("elevated_platform_user");
            req.setPassword("Pass1234!");
            req.setRole(Role.PLATFORM_ADMIN);

            mockMvc.perform(post("/api/auth/users")
                            .header("Authorization", "Bearer " + tokenAdminA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company administrator cannot reset password of a user in another company")
        void companyAdminCannotResetOtherCompanyUserPassword() throws Exception {
            mockMvc.perform(put("/api/auth/reset-password/bob_beta")
                            .header("Authorization", "Bearer " + tokenAdminA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPassword": "HijackedPassword123!"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Inactive company users cannot access protected endpoints")
        void inactiveCompanyUsersAreBlocked() throws Exception {
            // Deactivate Tenant B
            tenantB.setActive(false);
            companyRepository.save(tenantB);

            // Bob's token was valid, but company is now inactive -> Request rejected
            mockMvc.perform(get("/api/travel-requests")
                            .header("Authorization", "Bearer " + tokenEmployeeB))
                    .andExpect(status().isForbidden());

            // Bob tries to log in -> 401 Unauthorized
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "bob_beta", "password": "SecretPass123!"}
                                    """))
                    .andExpect(status().isUnauthorized());

            // Restore Tenant B for other tests
            tenantB.setActive(true);
            companyRepository.save(tenantB);
        }

        @Test
        @DisplayName("Inactive or disabled user cannot authenticate or perform operations")
        void disabledUserIsBlocked() throws Exception {
            User disabledUser = createOrGetUser("disabled_alice", Role.EMPLOYEE, tenantA, null);
            String disabledToken = jwtService.generateToken(disabledUser);

            // Disable account
            disabledUser.setActive(false);
            userRepository.save(disabledUser);

            // API request with token -> blocked
            mockMvc.perform(get("/api/travel-requests")
                            .header("Authorization", "Bearer " + disabledToken))
                    .andExpect(status().isForbidden());

            // Login attempt -> 401
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username": "disabled_alice", "password": "SecretPass123!"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // SECTION C: BUSINESS LOGIC & AUDIT INTEGRITY
    // =========================================================================

    @Nested
    @DisplayName("Business Logic & Audit Verification")
    class BusinessLogicTests {

        @Test
        @DisplayName("Duplicate company code or admin username is rejected with 409 Conflict")
        void duplicateCompanyRegistrationRejected() throws Exception {
            CompanyRegistrationRequest req = new CompanyRegistrationRequest();
            req.setName("Alpha Corp Clone");
            req.setCode("ALPHA_CORP"); // Existing code
            req.setDomain("alphaclone.com");
            req.setAdminUsername("new_admin_user");
            req.setAdminPassword("Password123!");

            mockMvc.perform(post("/api/companies/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict());

            CompanyRegistrationRequest req2 = new CompanyRegistrationRequest();
            req2.setName("Brand New Corp");
            req2.setCode("BRAND_NEW");
            req2.setAdminUsername("admin_alpha"); // Existing username
            req2.setAdminPassword("Password123!");

            mockMvc.perform(post("/api/companies/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req2)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Invalid expense transitions and invalid amounts are rejected")
        void invalidExpenseTransitionsAndAmountsRejected() throws Exception {
            // 1. Submit expense with amount <= 0
            String invalidAmountPayload = """
                    {
                        "category": "MEALS",
                        "amount": -50.00,
                        "currency": "INR",
                        "expenseDate": "2026-10-26",
                        "merchant": "Cafe Coffee Day"
                    }
                    """;

            mockMvc.perform(post("/api/expenses")
                            .header("Authorization", "Bearer " + tokenEmployeeA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidAmountPayload))
                    .andExpect(status().isBadRequest());

            // 2. Submit valid expense
            Expense exp = new Expense();
            exp.setCompany(tenantA);
            exp.setEmployee(employeeA);
            exp.setCategory(ExpenseCategory.MEALS);
            exp.setAmount(new BigDecimal("650.00"));
            exp.setCurrency("INR");
            exp.setExpenseDate(LocalDate.now());
            exp.setMerchant("Dominos");
            exp.setStatus(ExpenseStatus.SUBMITTED);
            exp = expenseRepository.save(exp);

            // 3. Attempt reimbursement on SUBMITTED expense (before approval) -> 400
            mockMvc.perform(patch("/api/expenses/" + exp.getId() + "/reimburse")
                            .header("Authorization", "Bearer " + tokenTravelDeskA))
                    .andExpect(status().isBadRequest());

            // 4. Approve expense
            mockMvc.perform(patch("/api/expenses/" + exp.getId() + "/approve")
                            .header("Authorization", "Bearer " + tokenApproverA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"comment": "Approved expense for reimbursement"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")));

            // 5. Attempt second approval on already approved expense -> 400
            mockMvc.perform(patch("/api/expenses/" + exp.getId() + "/approve")
                            .header("Authorization", "Bearer " + tokenApproverA))
                    .andExpect(status().isBadRequest());

            // 6. Reimburse expense -> records audit fields
            mockMvc.perform(patch("/api/expenses/" + exp.getId() + "/reimburse")
                            .header("Authorization", "Bearer " + tokenTravelDeskA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("REIMBURSED")))
                    .andExpect(jsonPath("$.reimbursedByName", is("desk_alpha")))
                    .andExpect(jsonPath("$.reimbursedAt", notNullValue()));

            // 7. Attempt to reimburse again -> 400
            mockMvc.perform(patch("/api/expenses/" + exp.getId() + "/reimburse")
                            .header("Authorization", "Bearer " + tokenTravelDeskA))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // SECTION D: END-TO-END REGRESSION
    // =========================================================================

    @Nested
    @DisplayName("End-to-End Regression Workflows")
    class RegressionTests {

        @Test
        @DisplayName("Full travel request and booking lifecycle functions smoothly within company")
        void fullTravelRequestAndBookingLifecycle() throws Exception {
            // 1. Employee submits travel request
            String trPayload = """
                    {
                        "tripType": "ONE_WAY",
                        "fromLocation": "Mumbai",
                        "toLocation": "Hyderabad",
                        "travelDate": "2026-11-15",
                        "projectName": "Annual Conference Alpha",
                        "reason": "Technical Speaker"
                    }
                    """;

            String trResponseStr = mockMvc.perform(post("/api/travel-requests")
                            .header("Authorization", "Bearer " + tokenEmployeeA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(trPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andReturn().getResponse().getContentAsString();

            long savedRequestId = objectMapper.readTree(trResponseStr).get("id").asLong();

            // 2. Approver approves
            ApprovalDecision dec = new ApprovalDecision();
            dec.setComment("Travel approved");
            mockMvc.perform(patch("/api/travel-requests/" + savedRequestId + "/approve")
                            .header("Authorization", "Bearer " + tokenApproverA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dec)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("APPROVED")));

            // 3. Travel Desk creates booking
            String bookingPayload = """
                    {
                        "travelRequest": { "id": %d },
                        "bookingType": "FLIGHT",
                        "bookingReference": "6E-7890",
                        "provider": "IndiGo",
                        "cost": 5200.00,
                        "savings": 300.00
                    }
                    """.formatted(savedRequestId);

            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", "Bearer " + tokenTravelDeskA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookingReference", is("6E-7890")));
        }
    }
}
