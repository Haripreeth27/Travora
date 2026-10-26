package com.traveldesk.backend.user;

public class TokenVerificationResponse {

    private boolean valid;
    private String username;
    private String name;
    private String companyName;
    private String message;

    public TokenVerificationResponse() {}

    public TokenVerificationResponse(boolean valid, String username, String name, String companyName, String message) {
        this.valid = valid;
        this.username = username;
        this.name = name;
        this.companyName = companyName;
        this.message = message;
    }

    public static TokenVerificationResponse valid(String username, String name, String companyName) {
        return new TokenVerificationResponse(true, username, name, companyName, "Token is valid");
    }

    public static TokenVerificationResponse invalid(String message) {
        return new TokenVerificationResponse(false, null, null, null, message);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
