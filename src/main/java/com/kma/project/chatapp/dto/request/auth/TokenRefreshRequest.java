package com.kma.project.chatapp.dto.request.auth;

import javax.validation.constraints.NotBlank;

public class TokenRefreshRequest {

    @NotBlank(message = "{error.refresh-token-not-null}")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
