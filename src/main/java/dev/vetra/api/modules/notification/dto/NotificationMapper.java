package dev.vetra.api.modules.notification.dto;

import dev.vetra.api.modules.notification.domain.Notification;

/**
 * Static mapping between Notification domain objects and DTOs.
 * DTOs do not leak into the domain layer.
 */
public final class NotificationMapper {

    private NotificationMapper() {
        // utility class
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.recipientUserId(),
                notification.channel().name(),
                notification.type(),
                notification.status().name(),
                notification.subject(),
                notification.payload(),
                notification.sentAt(),
                notification.createdAt()
        );
    }
}
