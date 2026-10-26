package com.traveldesk.backend.user;

import com.traveldesk.backend.auth.Role;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String name;
    private Role role;
    private boolean active;
    private String status;
    private String employeeId;
    private String department;
    private String designation;
    private Long companyId;
    private String companyCode;
    private String companyName;
    private LocalDateTime createdAt;
    private boolean hasPendingActivation;

    public UserResponse() {}

    public UserResponse(
            Long id,
            String username,
            String name,
            Role role,
            boolean active,
            String status,
            String employeeId,
            String department,
            String designation,
            Long companyId,
            String companyCode,
            String companyName,
            LocalDateTime createdAt,
            boolean hasPendingActivation
    ) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.role = role;
        this.active = active;
        this.status = status;
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.companyId = companyId;
        this.companyCode = companyCode;
        this.companyName = companyName;
        this.createdAt = createdAt;
        this.hasPendingActivation = hasPendingActivation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
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

    public boolean isHasPendingActivation() {
        return hasPendingActivation;
    }

    public void setHasPendingActivation(boolean hasPendingActivation) {
        this.hasPendingActivation = hasPendingActivation;
    }
}
