package com.vynedam.stockai.dto;

import com.vynedam.stockai.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(@NotBlank
            @Size(min = 2, max = 100) String name, @NotBlank
            @Email String email, @NotBlank
            @Size(min = 12, max = 128) String password, @Size(max = 50) String unitId) {

    }

    public record LoginRequest(@NotBlank
            @Email String email, @NotBlank String password) {

    }

    public record UserResponse(String id, String name, String email, Role role, String unitId) {

    }

    public record AuthResponse(String accessToken, String tokenType, UserResponse user) {

    }
}
