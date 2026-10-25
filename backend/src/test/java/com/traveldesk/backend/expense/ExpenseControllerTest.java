package com.traveldesk.backend.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traveldesk.backend.auth.JwtService;
import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.company.Company;
import com.traveldesk.backend.company.CompanyRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

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

    private Company companyA;
    private Company companyB;
    private Employee employeeA;
    private User employeeUserA;
    private User approverUserA;
    private User travelDeskUserA;
    private User approverUserB;

    private String employeeTokenA;
    private String approverTokenA;
    private String travelDeskTokenA;
    private String approverTokenB;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();
        userRepository.findByUsername("alice_a").ifPresent(userRepository::delete);
        userRepository.findByUsername("approver_a").ifPresent(userRepository::delete);
        userRepository.findByUsername("desk_a").ifPresent(userRepository::delete);
        userRepository.findByUsername("approver_b").ifPresent(userRepository::delete);
        employeeRepository.findByEmployeeId("EMP-A-01").ifPresent(employeeRepository::delete);
        companyRepository.findByCodeIgnoreCase("COMPA").ifPresent(companyRepository::delete);
        companyRepository.findByCodeIgnoreCase("COMPB").ifPresent(companyRepository::delete);

        // Tenant A
        companyA = companyRepository.save(new Company("Company A Corp", "COMPA", "comp-a.com"));
        employeeA = new Employee();
        employeeA.setEmployeeId("EMP-A-01");
        employeeA.setName("Alice Employee");
        employeeA.setDepartment("Engineering");
        employeeA.setDesignation("Software Engineer");
        employeeA.setCompany(companyA);
        employeeRepository.save(employeeA);

        employeeUserA = new User();
        employeeUserA.setUsername("alice_a");
        employeeUserA.setPassword(passwordEncoder.encode("Pass123!"));
        employeeUserA.setRole(Role.EMPLOYEE);
        employeeUserA.setEmployeeId("EMP-A-01");
        employeeUserA.setActive(true);
        employeeUserA.setCompany(companyA);
        userRepository.save(employeeUserA);
        employeeTokenA = jwtService.generateToken(employeeUserA);

        approverUserA = new User();
        approverUserA.setUsername("approver_a");
        approverUserA.setPassword(passwordEncoder.encode("Pass123!"));
        approverUserA.setRole(Role.APPROVER);
        approverUserA.setActive(true);
        approverUserA.setCompany(companyA);
        userRepository.save(approverUserA);
        approverTokenA = jwtService.generateToken(approverUserA);

        travelDeskUserA = new User();
        travelDeskUserA.setUsername("desk_a");
        travelDeskUserA.setPassword(passwordEncoder.encode("Pass123!"));
        travelDeskUserA.setRole(Role.TRAVEL_DESK);
        travelDeskUserA.setActive(true);
        travelDeskUserA.setCompany(companyA);
        userRepository.save(travelDeskUserA);
        travelDeskTokenA = jwtService.generateToken(travelDeskUserA);

        // Tenant B
        companyB = companyRepository.save(new Company("Company B Corp", "COMPB", "comp-b.com"));
        approverUserB = new User();
        approverUserB.setUsername("approver_b");
        approverUserB.setPassword(passwordEncoder.encode("Pass123!"));
        approverUserB.setRole(Role.APPROVER);
        approverUserB.setActive(true);
        approverUserB.setCompany(companyB);
        userRepository.save(approverUserB);
        approverTokenB = jwtService.generateToken(approverUserB);
    }

    @Test
    @DisplayName("Expense Claim Lifecycle: Submit -> Approve -> Reimburse")
    void testExpenseClaimLifecycle() throws Exception {
        // 1. Submit
        String reqJson = """
                {
                    "category": "MEALS",
                    "amount": 1250.50,
                    "currency": "INR",
                    "expenseDate": "2026-10-25",
                    "merchant": "Subway Restaurant",
                    "description": "Client business dinner"
                }
                """;

        String res = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + employeeTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category", is("MEALS")))
                .andExpect(jsonPath("$.amount", is(1250.50)))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.employeeName", is("Alice Employee")))
                .andExpect(jsonPath("$.companyName", is("Company A Corp")))
                .andReturn().getResponse().getContentAsString();

        Long expenseId = objectMapper.readTree(res).get("id").asLong();

        // 2. Approver A approves
        ExpenseApprovalRequest approval = new ExpenseApprovalRequest("Approved per corporate meal policy.");
        mockMvc.perform(patch("/api/expenses/" + expenseId + "/approve")
                        .header("Authorization", "Bearer " + approverTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approval)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approverName", is("approver_a")))
                .andExpect(jsonPath("$.approvalComment", is("Approved per corporate meal policy.")));

        // 3. Travel Desk A reimburses
        mockMvc.perform(patch("/api/expenses/" + expenseId + "/reimburse")
                        .header("Authorization", "Bearer " + travelDeskTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REIMBURSED")));
    }

    @Test
    @DisplayName("Strict Multi-Tenancy: Approver from Company B CANNOT view or approve Company A's expense claim")
    void testTenantBoundaryEnforcement() throws Exception {
        // Create an expense under Company A
        Expense expenseA = new Expense();
        expenseA.setCompany(companyA);
        expenseA.setEmployee(employeeA);
        expenseA.setCategory(ExpenseCategory.TAXI);
        expenseA.setAmount(new BigDecimal("450.00"));
        expenseA.setExpenseDate(LocalDate.now());
        expenseA.setMerchant("Uber India");
        expenseA.setStatus(ExpenseStatus.SUBMITTED);
        Expense savedA = expenseRepository.save(expenseA);

        // Tenant B approver attempts to view Tenant A's expense -> 404 NOT FOUND
        mockMvc.perform(get("/api/expenses/" + savedA.getId())
                        .header("Authorization", "Bearer " + approverTokenB))
                .andExpect(status().isNotFound());

        // Tenant B approver attempts to approve Tenant A's expense -> 404 NOT FOUND
        ExpenseApprovalRequest approval = new ExpenseApprovalRequest("Attempted cross-tenant approval");
        mockMvc.perform(patch("/api/expenses/" + savedA.getId() + "/approve")
                        .header("Authorization", "Bearer " + approverTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approval)))
                .andExpect(status().isNotFound());
    }
}
