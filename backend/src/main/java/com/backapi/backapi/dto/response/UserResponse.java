package com.backapi.backapi.dto.response;

import com.backapi.backapi.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
}

