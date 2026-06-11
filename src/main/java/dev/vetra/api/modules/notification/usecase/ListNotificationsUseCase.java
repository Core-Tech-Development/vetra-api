package dev.vetra.api.modules.notification.usecase;

import dev.vetra.api.modules.notification.domain.Notification;
import dev.vetra.api.modules.notification.dto.NotificationMapper;
import dev.vetra.api.modules.notification.dto.NotificationResponse;
import dev.vetra.api.modules.notification.repository.NotificationRepository;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Lists notifications for a specific recipient with pagination.
 */
@ApplicationScoped
public class ListNotificationsUseCase {

    private static final Logger LOG = Logger.getLogger(ListNotificationsUseCase.class);

    private final NotificationRepository notificationRepository;

    @Inject
    public ListNotificationsUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Uni<PageResponse<NotificationResponse>> execute(String recipientUserId, PageRequest pageRequest) {
        LOG.debugf("Listing notifications: userId=%s, page=%d, size=%d", recipientUserId, pageRequest.page(), pageRequest.size());
        Uni<List<Notification>> notificationsUni = notificationRepository
                .findByRecipientUserId(recipientUserId, pageRequest.offset(), pageRequest.size());
        Uni<Long> countUni = notificationRepository.countByRecipientUserId(recipientUserId);

        return Uni.combine().all().unis(notificationsUni, countUni)
                .asTuple()
                .map(tuple -> {
                    List<NotificationResponse> content = tuple.getItem1().stream()
                            .map(NotificationMapper::toResponse)
                            .toList();
                    long total = tuple.getItem2();
                    return PageResponse.of(content, total, pageRequest);
                });
    }
}
