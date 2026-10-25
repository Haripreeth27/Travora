package com.traveldesk.backend.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(
        Long id,
        Long companyId,
        String companyName,
        Long travelRequestId,
        Long employeeId,
        String employeeName,
        String employeeCode,
        ExpenseCategory category,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String merchant,
        String description,
        String receiptUrl,
        String receiptName,
        ExpenseStatus status,
        String approverName,
        String approvalComment,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {
    public static ExpenseResponse fromEntity(Expense e) {
        if (e == null) return null;
        return new ExpenseResponse(
                e.getId(),
                e.getCompany() != null ? e.getCompany().getId() : null,
                e.getCompany() != null ? e.getCompany().getName() : null,
                e.getTravelRequest() != null ? e.getTravelRequest().getId() : null,
                e.getEmployee() != null ? e.getEmployee().getId() : null,
                e.getEmployee() != null ? e.getEmployee().getName() : null,
                e.getEmployee() != null ? e.getEmployee().getEmployeeId() : null,
                e.getCategory(),
                e.getAmount(),
                e.getCurrency(),
                e.getExpenseDate(),
                e.getMerchant(),
                e.getDescription(),
                e.getReceiptUrl(),
                e.getReceiptName(),
                e.getStatus(),
                e.getApprover() != null ? e.getApprover().getUsername() : null,
                e.getApprovalComment(),
                e.getApprovedAt(),
                e.getCreatedAt()
        );
    }
}
