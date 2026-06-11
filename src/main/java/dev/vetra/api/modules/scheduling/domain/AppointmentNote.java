package dev.vetra.api.modules.scheduling.domain;

import java.time.Instant;
import java.util.UUID;

public record AppointmentNote(
        UUID id,
        UUID appointmentId,
        String authorUserId,
        String title,
        String content,
        Instant createdAt
) {
    public static AppointmentNote create(UUID appointmentId, String authorUserId, String title, String content) {
        return new AppointmentNote(
                UUID.randomUUID(),
                appointmentId,
                authorUserId,
                title,
                content,
                Instant.now()
        );
    }

    public static AppointmentNote restore(UUID id, UUID appointmentId, String authorUserId, String title, String content, Instant createdAt) {
        return new AppointmentNote(id, appointmentId, authorUserId, title, content, createdAt);
    }
}
