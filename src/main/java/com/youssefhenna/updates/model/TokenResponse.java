package com.youssefhenna.updates.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
    private String token;

    @JsonProperty("access_token")
    private String accessToken;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String resolveToken() {
        return token != null ? token : accessToken;
    }
}
