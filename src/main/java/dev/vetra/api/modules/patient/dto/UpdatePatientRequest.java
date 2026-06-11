package dev.vetra.api.modules.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePatientRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Species is required")
        @Size(max = 50, message = "Species must be at most 50 characters")
        String species,

        @Size(max = 100, message = "Breed must be at most 100 characters")
        String breed,

        @Size(max = 20, message = "Sex must be at most 20 characters")
        String sex,

        String birthDate,

        BigDecimal weightKg,

        Boolean neutered,

        @Size(max = 50, message = "Microchip must be at most 50 characters")
        String microchip,

        @Size(max = 2000, message = "Clinical notes must be at most 2000 characters")
        String clinicalNotes
) {
}
