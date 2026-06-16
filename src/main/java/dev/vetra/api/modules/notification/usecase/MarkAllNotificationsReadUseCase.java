package dev.vetra.api.modules.notification.usecase;

import dev.vetra.api.modules.notification.repository.NotificationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Marks all unread notifications as read for a given recipient.
 */
@ApplicationScoped
public class MarkAllNotificationsReadUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public MarkAllNotificationsReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Uni<Void> execute(String recipientUserId) {
        return notificationRepository.markAllAsRead(recipientUserId);
    }
}
