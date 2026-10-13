package com.traveldesk.backend.bulkbooking;

public class ReviewRequest {

    private String status; // "APPROVED" or "NOTED"
    private String comment;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
