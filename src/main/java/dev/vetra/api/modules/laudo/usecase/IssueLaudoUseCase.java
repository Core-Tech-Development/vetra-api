package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.billing.usecase.CreateBillingRecordUseCase;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.domain.AppointmentStatus;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues a draft laudo by transitioning status from DRAFT to ISSUED.
 * Sets issuedAt to the current timestamp.
 * Also transitions the linked appointment to REPORT_ISSUED.
 */
@ApplicationScoped
public class IssueLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(IssueLaudoUseCase.class);

    private final LaudoRepository laudoRepository;
    private final AppointmentRepository appointmentRepository;
    private final ExamRequestRepository examRequestRepository;
    private final CreateBillingRecordUseCase createBillingRecordUseCase;
    private final NotificationService notificationService;

    @Inject
    public IssueLaudoUseCase(LaudoRepository laudoRepository,
                              AppointmentRepository appointmentRepository,
                              ExamRequestRepository examRequestRepository,
                              CreateBillingRecordUseCase createBillingRecordUseCase,
                              NotificationService notificationService) {
        this.laudoRepository = laudoRepository;
        this.appointmentRepository = appointmentRepository;
        this.examRequestRepository = examRequestRepository;
        this.createBillingRecordUseCase = createBillingRecordUseCase;
        this.notificationService = notificationService;
    }

    public Uni<Laudo> execute(UUID laudoId) {
        return laudoRepository.findById(laudoId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", laudoId));
                    }

                    Laudo existing = opt.get();

                    if (existing.status() != LaudoStatus.DRAFT) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "Laudo can only be issued from DRAFT status. Current status: " + existing.status())
                        );
                    }

                    Instant now = Instant.now();
                    Laudo issued = Laudo.restore(
                            existing.id(),
                            existing.appointmentId(),
                            existing.specialistId(),
                            LaudoStatus.ISSUED,
                            existing.findings(),
                            existing.conclusion(),
                            existing.recommendations(),
                            existing.pdfStorageKey(),
                            now,
                            existing.createdAt(),
                            now
                    );

                    LOG.infof("Issuing laudo: id=%s, appointmentId=%s", laudoId, existing.appointmentId());
                    return laudoRepository.update(issued)
                            .flatMap(savedLaudo ->
                                    appointmentRepository.updateStatus(savedLaudo.appointmentId(), AppointmentStatus.REPORT_ISSUED)
                                            .onItem().invoke(() -> LOG.infof("Appointment %s transitioned to REPORT_ISSUED", savedLaudo.appointmentId()))
                                            .onFailure().invoke(err -> LOG.warnf(err, "Failed to update appointment status for %s", savedLaudo.appointmentId()))
                                            .onFailure().recoverWithNull()
                                            .map(v -> savedLaudo)
                            )
                            .call(savedLaudo -> notifyClinicLaudoIssued(savedLaudo))
                            .flatMap(savedLaudo -> triggerBilling(savedLaudo)
                                    .map(v -> savedLaudo));
                });
    }

    private Uni<Void> notifyClinicLaudoIssued(Laudo laudo) {
        return appointmentRepository.findById(laudo.appointmentId())
                .flatMap(aptOpt -> {
                    if (aptOpt.isEmpty()) return Uni.createFrom().voidItem();
                    return examRequestRepository.findById(aptOpt.get().examRequestId())
                            .flatMap(erOpt -> {
                                if (erOpt.isEmpty()) return Uni.createFrom().voidItem();
                                return notificationService.notifyClinicAdmins(
                                        erOpt.get().clinicId(),
                                        NotificationType.LAUDO_ISSUED,
                                        "Laudo emitido",
                                        null, laudo.id(), "LAUDO");
                            });
                })
                .onFailure().invoke(err -> LOG.warnf(err, "Failed to notify clinic about laudo %s", laudo.id()))
                .onFailure().recoverWithNull()
                .replaceWithVoid();
    }

    private Uni<Void> triggerBilling(Laudo laudo) {
        return appointmentRepository.findById(laudo.appointmentId())
                .flatMap(aptOpt -> {
                    if (aptOpt.isEmpty()) {
                        LOG.warnf("Appointment %s not found for billing trigger", laudo.appointmentId());
                        return Uni.createFrom().voidItem();
                    }
                    var appointment = aptOpt.get();
                    return examRequestRepository.findById(appointment.examRequestId())
                            .flatMap(erOpt -> {
                                if (erOpt.isEmpty()) {
                                    LOG.warnf("ExamRequest %s not found for billing trigger", appointment.examRequestId());
                                    return Uni.createFrom().voidItem();
                                }
                                UUID clinicId = erOpt.get().clinicId();
                                LOG.infof("Triggering billing for laudo=%s, appointment=%s, clinic=%s, specialist=%s",
                                        laudo.id(), laudo.appointmentId(), clinicId, laudo.specialistId());
                                return createBillingRecordUseCase.execute(
                                                laudo.id(), laudo.appointmentId(), clinicId, laudo.specialistId())
                                        .onItem().invoke(record -> LOG.infof("Billing record created: %s", record.id()))
                                        .onFailure().invoke(err -> LOG.errorf(err, "Failed to create billing record for laudo %s", laudo.id()))
                                        .onFailure().recoverWithNull()
                                        .replaceWithVoid();
                            });
                });
    }
}
