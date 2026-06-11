package dev.vetra.api.modules.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a notification.
 * Pure Java record — no framework annotations.
 */
public record Notification(
        UUID id,
        String recipientUserId,
        NotificationChannel channel,
        String type,
        NotificationStatus status,
        String subject,
        String payload,
        Instant sentAt,
        Instant createdAt
) {

    /**
     * Factory for creating a new notification with PENDING status.
     */
    public static Notification create(String recipientUserId, NotificationChannel channel,
                                       String type, String subject, String payload) {
        return new Notification(
                UUID.randomUUID(),
                recipientUserId,
                channel,
                type,
                NotificationStatus.PENDING,
                subject,
                payload,
                null,
                Instant.now()
        );
    }

    /**
     * Factory for restoring a notification from persistence.
     */
    public static Notification restore(UUID id, String recipientUserId, NotificationChannel channel,
                                        String type, NotificationStatus status, String subject,
                                        String payload, Instant sentAt, Instant createdAt) {
        return new Notification(id, recipientUserId, channel, type, status, subject,
                payload, sentAt, createdAt);
    }
}
