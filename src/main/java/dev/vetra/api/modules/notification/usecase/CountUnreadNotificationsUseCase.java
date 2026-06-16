package dev.vetra.api.modules.notification.usecase;

import dev.vetra.api.modules.notification.repository.NotificationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Counts unread notifications for a given recipient.
 */
@ApplicationScoped
public class CountUnreadNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public CountUnreadNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Uni<Long> execute(String recipientUserId) {
        return notificationRepository.countUnreadByRecipientUserId(recipientUserId);
    }
}
