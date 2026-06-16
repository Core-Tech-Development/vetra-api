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
        Instant createdAt,
        UUID referenceId,
        String referenceType,
        Instant readAt
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
                Instant.now(),
                null,
                null,
                null
        );
    }

    /**
     * Factory for creating a new notification with PENDING status and reference.
     */
    public static Notification create(String recipientUserId, NotificationChannel channel,
                                       String type, String subject, String payload,
                                       UUID referenceId, String referenceType) {
        return new Notification(
                UUID.randomUUID(),
                recipientUserId,
                channel,
                type,
                NotificationStatus.PENDING,
                subject,
                payload,
                null,
                Instant.now(),
                referenceId,
                referenceType,
                null
        );
    }

    /**
     * Factory for restoring a notification from persistence.
     */
    public static Notification restore(UUID id, String recipientUserId, NotificationChannel channel,
                                        String type, NotificationStatus status, String subject,
                                        String payload, Instant sentAt, Instant createdAt) {
        return new Notification(id, recipientUserId, channel, type, status, subject,
                payload, sentAt, createdAt, null, null, null);
    }

    /**
     * Factory for restoring a notification from persistence with all fields.
     */
    public static Notification restore(UUID id, String recipientUserId, NotificationChannel channel,
                                        String type, NotificationStatus status, String subject,
                                        String payload, Instant sentAt, Instant createdAt,
                                        UUID referenceId, String referenceType, Instant readAt) {
        return new Notification(id, recipientUserId, channel, type, status, subject,
                payload, sentAt, createdAt, referenceId, referenceType, readAt);
    }

    /**
     * Returns a copy with status set to SENT and sentAt set to now.
     */
    public Notification withSent() {
        return new Notification(id, recipientUserId, channel, type, NotificationStatus.SENT,
                subject, payload, Instant.now(), createdAt, referenceId, referenceType, readAt);
    }

    /**
     * Returns a copy with readAt set to now.
     */
    public Notification withRead() {
        return new Notification(id, recipientUserId, channel, type, status,
                subject, payload, sentAt, createdAt, referenceId, referenceType, Instant.now());
    }
}
