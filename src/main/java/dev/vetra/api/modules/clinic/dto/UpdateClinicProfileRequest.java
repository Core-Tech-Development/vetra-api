package dev.vetra.api.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClinicProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        String address,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 2, message = "State must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z]{2}$", message = "State must be a valid 2-letter code")
        String state
) {}
