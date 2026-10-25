package com.traveldesk.backend.expense;

public class ExpenseApprovalRequest {
    private String comment;

    public ExpenseApprovalRequest() {}

    public ExpenseApprovalRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
