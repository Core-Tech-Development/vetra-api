package dev.vetra.api.modules.notification.usecase;

import dev.vetra.api.modules.notification.repository.NotificationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Marks a single notification as read.
 */
@ApplicationScoped
public class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    @Inject
    public MarkNotificationReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Uni<Void> execute(UUID notificationId) {
        return notificationRepository.markAsRead(notificationId);
    }
}
