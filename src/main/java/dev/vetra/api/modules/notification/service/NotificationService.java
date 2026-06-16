package dev.vetra.api.modules.notification.service;

import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.modules.notification.domain.Notification;
import dev.vetra.api.modules.notification.domain.NotificationChannel;
import dev.vetra.api.modules.notification.repository.NotificationRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Central dispatch service for creating and sending in-app notifications.
 * All send operations use fire-and-forget: failures are logged but never propagated.
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final SpecialistRepository specialistRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository,
                                ClinicStaffRepository clinicStaffRepository,
                                SpecialistRepository specialistRepository) {
        this.notificationRepository = notificationRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.specialistRepository = specialistRepository;
    }

    /**
     * Creates an IN_APP notification immediately (fire-and-forget).
     * Failures are logged but do NOT propagate.
     */
    public Uni<Void> sendInApp(String recipientUserId, String type, String subject,
                                String payload, UUID referenceId, String referenceType) {
        Notification notification = Notification.create(
                recipientUserId, NotificationChannel.IN_APP, type, subject, payload,
                referenceId, referenceType);
        Notification sent = notification.withSent();
        return notificationRepository.save(sent)
                .invoke(n -> LOG.infof("Notification sent: type=%s, recipient=%s", type, recipientUserId))
                .onFailure().invoke(err -> LOG.errorf(err, "Failed notification: type=%s", type))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    /**
     * Notifies all active staff of a clinic.
     */
    public Uni<Void> notifyClinicAdmins(UUID clinicId, String type, String subject,
                                         String payload, UUID referenceId, String referenceType) {
        return clinicStaffRepository.findAdminUserIdsByClinicId(clinicId)
                .flatMap(userIds -> {
                    if (userIds.isEmpty()) return Uni.createFrom().voidItem();
                    List<Uni<Void>> sends = userIds.stream()
                            .map(uid -> sendInApp(uid, type, subject, payload, referenceId, referenceType))
                            .toList();
                    return Uni.join().all(sends).andFailFast().replaceWithVoid();
                })
                .onFailure().invoke(err -> LOG.errorf(err, "Failed notifyClinicAdmins: clinicId=%s", clinicId))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    /**
     * Notifies all active specialists (e.g. new exam request available).
     */
    public Uni<Void> notifyAllActiveSpecialists(String type, String subject,
                                                 String payload, UUID referenceId, String referenceType) {
        return specialistRepository.findActiveSpecialistIds()
                .flatMap(ids -> {
                    if (ids.isEmpty()) return Uni.createFrom().voidItem();
                    List<Uni<Void>> sends = ids.stream()
                            .map(id -> notifySpecialist(id, type, subject, payload, referenceId, referenceType))
                            .toList();
                    return Uni.join().all(sends).andFailFast().replaceWithVoid();
                })
                .onFailure().invoke(err -> LOG.errorf(err, "Failed notifyAllActiveSpecialists: type=%s", type))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    /**
     * Notifies a specialist by their specialist ID (resolves userId internally).
     */
    public Uni<Void> notifySpecialist(UUID specialistId, String type, String subject,
                                       String payload, UUID referenceId, String referenceType) {
        return specialistRepository.findById(specialistId)
                .flatMap(opt -> {
                    if (opt.isEmpty() || opt.get().userId() == null) return Uni.createFrom().voidItem();
                    return sendInApp(opt.get().userId(), type, subject, payload, referenceId, referenceType);
                })
                .onFailure().invoke(err -> LOG.errorf(err, "Failed notifySpecialist: id=%s", specialistId))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }
}
