package dev.vetra.api.modules.scheduling.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentRequest(

        @NotBlank(message = "Cancellation reason is required")
        String reason
) {
}
