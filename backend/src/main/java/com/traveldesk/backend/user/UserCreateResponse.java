package com.traveldesk.backend.user;

public class UserCreateResponse extends UserResponse {

    private String activationToken;
    private String activationUrl;

    public UserCreateResponse() {
        super();
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public String getActivationUrl() {
        return activationUrl;
    }

    public void setActivationUrl(String activationUrl) {
        this.activationUrl = activationUrl;
    }
}
