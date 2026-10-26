package com.traveldesk.backend.expense;

import com.traveldesk.backend.auth.Role;
import com.traveldesk.backend.auth.User;
import com.traveldesk.backend.auth.UserRepository;
import com.traveldesk.backend.employee.Employee;
import com.traveldesk.backend.employee.EmployeeRepository;
import com.traveldesk.backend.travelrequest.TravelRequest;
import com.traveldesk.backend.travelrequest.TravelRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TravelRequestRepository travelRequestRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            TravelRequestRepository travelRequestRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.travelRequestRepository = travelRequestRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        User currentUser = getCurrentUser();
        Employee employee;

        if (currentUser.getRole() == Role.EMPLOYEE) {
            employee = getEmployeeForUser(currentUser);
        } else {
            if (request.getEmployeeId() == null) {
                if (currentUser.getEmployeeId() != null) {
                    employee = getEmployeeForUser(currentUser);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee ID is required");
                }
            } else {
                employee = employeeRepository.findById(request.getEmployeeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
            }
        }

        // Validate tenant boundaries
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && employee.getCompany() != null) {
            if (!employee.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create expense for an employee from another company");
            }
        }

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expense amount must be greater than zero");
        }

        TravelRequest travelRequest = null;
        if (request.getTravelRequestId() != null) {
            travelRequest = travelRequestRepository.findById(request.getTravelRequestId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel request not found"));

            if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && travelRequest.getCompany() != null) {
                if (!travelRequest.getCompany().getId().equals(currentUser.getCompany().getId())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel request not found");
                }
            }

            if (currentUser.getRole() == Role.EMPLOYEE) {
                if (travelRequest.getEmployee() == null || !travelRequest.getEmployee().getId().equals(employee.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot link an expense to another employee's travel request");
                }
            }
        }

        Expense expense = new Expense();
        expense.setCompany(currentUser.getCompany() != null ? currentUser.getCompany() : employee.getCompany());
        expense.setEmployee(employee);
        expense.setTravelRequest(travelRequest);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "INR");
        expense.setExpenseDate(request.getExpenseDate());
        expense.setMerchant(request.getMerchant());
        expense.setDescription(request.getDescription());
        expense.setReceiptUrl(request.getReceiptUrl());
        expense.setReceiptName(request.getReceiptName());
        expense.setStatus(ExpenseStatus.SUBMITTED);
        expense.setCreatedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
    }

    public List<ExpenseResponse> getAllExpenses(ExpenseStatus statusFilter) {
        User currentUser = getCurrentUser();
        List<Expense> list;

        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(currentUser);
            if (currentUser.getCompany() != null) {
                list = expenseRepository.findByCompanyIdAndEmployeeId(currentUser.getCompany().getId(), employee.getId());
            } else {
                list = expenseRepository.findByEmployeeId(employee.getId());
            }
        } else if (currentUser.getRole() == Role.PLATFORM_ADMIN) {
            list = expenseRepository.findAll();
        } else if (currentUser.getCompany() != null) {
            list = expenseRepository.findByCompanyId(currentUser.getCompany().getId());
        } else {
            list = expenseRepository.findAll();
        }

        if (statusFilter != null) {
            list = list.stream().filter(e -> e.getStatus() == statusFilter).toList();
        }

        return list.stream().map(ExpenseResponse::fromEntity).toList();
    }

    public ExpenseResponse getExpenseById(Long id) {
        Expense expense = findAndAuthorizeExpense(id);
        return ExpenseResponse.fromEntity(expense);
    }

    @Transactional
    public ExpenseResponse approveExpense(Long id, String comment) {
        User currentUser = getCurrentUser();
        authorizeApprover(currentUser);

        Expense expense = findAndAuthorizeExpense(id);
        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only submitted expenses can be approved");
        }

        expense.setStatus(ExpenseStatus.APPROVED);
        expense.setApprover(currentUser);
        expense.setApprovalComment(comment);
        expense.setApprovedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public ExpenseResponse rejectExpense(Long id, String comment) {
        User currentUser = getCurrentUser();
        authorizeApprover(currentUser);

        Expense expense = findAndAuthorizeExpense(id);
        if (expense.getStatus() != ExpenseStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only submitted expenses can be rejected");
        }

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setApprover(currentUser);
        expense.setApprovalComment(comment);
        expense.setApprovedAt(LocalDateTime.now());

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public ExpenseResponse reimburseExpense(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.TRAVEL_DESK &&
            currentUser.getRole() != Role.ADMIN &&
            currentUser.getRole() != Role.COMPANY_ADMIN &&
            currentUser.getRole() != Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only travel desk or finance administrators can mark expenses as reimbursed");
        }

        Expense expense = findAndAuthorizeExpense(id);
        if (expense.getStatus() != ExpenseStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved expenses can be reimbursed");
        }

        expense.setStatus(ExpenseStatus.REIMBURSED);
        expense.setReimbursedBy(currentUser);
        expense.setReimbursedAt(LocalDateTime.now());
        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteExpense(Long id) {
        Expense expense = findAndAuthorizeExpense(id);
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (expense.getStatus() != ExpenseStatus.SUBMITTED && expense.getStatus() != ExpenseStatus.DRAFT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete an expense that has already been approved or reimbursed");
            }
        }

        expenseRepository.delete(expense);
    }

    private Expense findAndAuthorizeExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));

        User currentUser = getCurrentUser();

        // Enforce tenant boundary
        if (currentUser.getRole() != Role.PLATFORM_ADMIN && currentUser.getCompany() != null && expense.getCompany() != null) {
            if (!expense.getCompany().getId().equals(currentUser.getCompany().getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found");
            }
        }

        // If employee, enforce own-data boundary
        if (currentUser.getRole() == Role.EMPLOYEE) {
            Employee employee = getEmployeeForUser(currentUser);
            if (!expense.getEmployee().getId().equals(employee.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own expenses");
            }
        }

        return expense;
    }

    private void authorizeApprover(User user) {
        if (user.getRole() != Role.APPROVER &&
            user.getRole() != Role.ADMIN &&
            user.getRole() != Role.COMPANY_ADMIN &&
            user.getRole() != Role.PLATFORM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only designated approvers or administrators can review expenses");
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Employee getEmployeeForUser(User user) {
        if (user.getEmployeeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not linked to an employee profile");
        }
        return employeeRepository.findByEmployeeId(user.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked employee profile not found"));
    }
}
