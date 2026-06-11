package dev.vetra.api.modules.specialist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Size(max = 100, message = "Base city must be at most 100 characters")
        String baseCity,

        @Size(max = 2, message = "Base state must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Base state must be a valid 2-letter code")
        String baseState,

        Integer maxTravelRadiusKm,

        @NotNull(message = "Has own equipment flag is required")
        Boolean hasOwnEquipment,

        String bio
) {
}
