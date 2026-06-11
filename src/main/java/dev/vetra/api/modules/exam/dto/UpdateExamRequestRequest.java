package dev.vetra.api.modules.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExamRequestRequest(

        @NotBlank(message = "Exam type is required")
        @Size(max = 100, message = "Exam type must be at most 100 characters")
        String examType,

        @Size(max = 50, message = "Priority must be at most 50 characters")
        String priority,

        @Size(max = 500, message = "Diagnostic hypothesis must be at most 500 characters")
        String diagnosticHypothesis,

        @Size(max = 2000, message = "Clinical history must be at most 2000 characters")
        String clinicalHistory,

        @Size(max = 1000, message = "Additional notes must be at most 1000 characters")
        String additionalNotes
) {
}
