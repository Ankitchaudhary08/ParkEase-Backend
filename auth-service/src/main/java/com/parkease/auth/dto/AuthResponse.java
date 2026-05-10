package com.parkease.auth.dto;

import com.parkease.auth.entity.User;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String email;
    private String fullName;
    private User.Role role;
}
