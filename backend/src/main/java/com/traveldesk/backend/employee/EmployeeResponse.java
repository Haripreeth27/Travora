package com.traveldesk.backend.employee;

import java.time.LocalDateTime;

public class EmployeeResponse {

    private Long id;
    private String employeeId;
    private String name;
    private String department;
    private String designation;
    private String location;
    private String email;
    private String contactNumber;
    private boolean active;
    private Long companyId;
    private String companyCode;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmployeeResponse() {}

    public static EmployeeResponse fromEntity(Employee emp) {
        if (emp == null) return null;
        EmployeeResponse res = new EmployeeResponse();
        res.setId(emp.getId());
        res.setEmployeeId(emp.getEmployeeId());
        res.setName(emp.getName());
        res.setDepartment(emp.getDepartment());
        res.setDesignation(emp.getDesignation());
        res.setLocation(emp.getLocation());
        res.setEmail(emp.getEmail());
        res.setContactNumber(emp.getContactNumber());
        res.setActive(emp.isActive());
        res.setCreatedAt(emp.getCreatedAt());
        res.setUpdatedAt(emp.getUpdatedAt());
        if (emp.getCompany() != null) {
            res.setCompanyId(emp.getCompany().getId());
            res.setCompanyCode(emp.getCompany().getCode());
            res.setCompanyName(emp.getCompany().getName());
        }
        return res;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

