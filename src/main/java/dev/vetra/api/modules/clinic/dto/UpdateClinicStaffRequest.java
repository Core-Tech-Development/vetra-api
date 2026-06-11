package dev.vetra.api.modules.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClinicStaffRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "VETERINARIAN|SECRETARY", message = "Role must be VETERINARIAN or SECRETARY")
        String role
) {}
