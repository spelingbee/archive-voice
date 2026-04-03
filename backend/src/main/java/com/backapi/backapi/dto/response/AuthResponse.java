package com.backapi.backapi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private UserResponse user;
}