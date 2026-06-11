package dev.vetra.api.modules.laudo.dto;

import dev.vetra.api.modules.laudo.domain.Laudo;

/**
 * Static mapping between Laudo domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class LaudoMapper {

    private LaudoMapper() {
        // utility class
    }

    public static LaudoResponse toResponse(Laudo laudo) {
        return new LaudoResponse(
                laudo.id(),
                laudo.appointmentId(),
                laudo.specialistId(),
                laudo.status().name(),
                laudo.findings(),
                laudo.conclusion(),
                laudo.recommendations(),
                laudo.pdfStorageKey(),
                laudo.issuedAt(),
                laudo.createdAt(),
                laudo.updatedAt()
        );
    }
}
