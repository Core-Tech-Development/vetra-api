package dev.vetra.api.modules.laudo.dto;

/**
 * Response containing a presigned download URL for a laudo PDF.
 */
public record LaudoPdfUrlResponse(String downloadUrl) {}
