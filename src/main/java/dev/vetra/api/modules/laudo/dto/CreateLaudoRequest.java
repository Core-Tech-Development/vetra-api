package dev.vetra.api.modules.laudo.dto;

import jakarta.validation.constraints.Size;

public record CreateLaudoRequest(

        @Size(max = 10000, message = "Findings must be at most 10000 characters")
        String findings,

        @Size(max = 5000, message = "Conclusion must be at most 5000 characters")
        String conclusion,

        @Size(max = 5000, message = "Recommendations must be at most 5000 characters")
        String recommendations
) {
}
