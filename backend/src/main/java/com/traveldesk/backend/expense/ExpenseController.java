package com.traveldesk.backend.expense;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.createExpense(request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ExpenseResponse> getAllExpenses(
            @RequestParam(required = false) ExpenseStatus status
    ) {
        return expenseService.getAllExpenses(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ExpenseResponse getExpenseById(@PathVariable Long id) {
        return expenseService.getExpenseById(id);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public ExpenseResponse approveExpense(
            @PathVariable Long id,
            @RequestBody(required = false) ExpenseApprovalRequest request
    ) {
        String comment = request != null ? request.getComment() : null;
        return expenseService.approveExpense(id, comment);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('APPROVER', 'ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public ExpenseResponse rejectExpense(
            @PathVariable Long id,
            @RequestBody(required = false) ExpenseApprovalRequest request
    ) {
        String comment = request != null ? request.getComment() : null;
        return expenseService.rejectExpense(id, comment);
    }

    @PatchMapping("/{id}/reimburse")
    @PreAuthorize("hasAnyRole('TRAVEL_DESK', 'ADMIN', 'COMPANY_ADMIN', 'PLATFORM_ADMIN')")
    public ExpenseResponse reimburseExpense(@PathVariable Long id) {
        return expenseService.reimburseExpense(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }
}
