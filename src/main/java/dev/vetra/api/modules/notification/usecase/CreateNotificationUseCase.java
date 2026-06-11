package dev.vetra.api.modules.notification.usecase;

import dev.vetra.api.modules.notification.domain.Notification;
import dev.vetra.api.modules.notification.domain.NotificationChannel;
import dev.vetra.api.modules.notification.repository.NotificationRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Creates a new notification and saves it to the database with PENDING status.
 */
@ApplicationScoped
public class CreateNotificationUseCase {

    private static final Logger LOG = Logger.getLogger(CreateNotificationUseCase.class);

    private final NotificationRepository notificationRepository;

    @Inject
    public CreateNotificationUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Uni<Notification> execute(String recipientUserId, NotificationChannel channel,
                                      String type, String subject, String payload) {
        Notification notification = Notification.create(recipientUserId, channel, type, subject, payload);

        LOG.infof("Creating notification: id=%s, recipient=%s, channel=%s, type=%s",
                notification.id(), recipientUserId, channel, type);

        return notificationRepository.save(notification);
    }
}
