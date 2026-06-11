package dev.vetra.api.modules.specialist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddCoverageAreaRequest(

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(min = 2, max = 2, message = "State must be exactly 2 characters")
        @Pattern(regexp = "^[A-Z]{2}$", message = "State must be a valid 2-letter code")
        String state,

        Integer radiusKm
) {
}
