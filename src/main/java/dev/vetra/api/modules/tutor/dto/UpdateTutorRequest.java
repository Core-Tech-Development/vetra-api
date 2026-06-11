package dev.vetra.api.modules.tutor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTutorRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 20, message = "Document must be at most 20 characters")
        String document,

        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 2, message = "State must be at most 2 characters")
        String state,

        @Size(max = 10, message = "Zip code must be at most 10 characters")
        String zipCode
) {
}
