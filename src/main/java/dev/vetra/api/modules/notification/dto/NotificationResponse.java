package dev.vetra.api.modules.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientUserId,
        String channel,
        String type,
        String status,
        String subject,
        String payload,
        Instant sentAt,
        Instant createdAt,
        UUID referenceId,
        String referenceType,
        boolean read,
        Instant readAt
) {
}
