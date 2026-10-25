package com.traveldesk.backend.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompanyRegistrationRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Company code is required")
    @Size(min = 2, max = 50, message = "Company code must be between 2 and 50 alphanumeric characters")
    private String code;

    private String domain;

    @NotBlank(message = "Admin username is required")
    private String adminUsername;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String adminPassword;

    private String adminEmail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }
}
