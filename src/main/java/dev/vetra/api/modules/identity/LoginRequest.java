package dev.vetra.api.modules.identity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

@RegisterForReflection
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "Password is required")
        String password,
        @NotBlank(message = "CAPTCHA verification is required")
        String captcha
) {}
