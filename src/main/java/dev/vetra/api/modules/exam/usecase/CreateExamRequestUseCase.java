package dev.vetra.api.modules.exam.usecase;

import dev.vetra.api.modules.exam.domain.ExamRequest;
import dev.vetra.api.modules.exam.dto.CreateExamRequestRequest;
import dev.vetra.api.modules.exam.dto.ExamRequestMapper;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.audit.usecase.LogAuditEventUseCase;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Creates a new exam request for a patient within a clinic.
 * Validates that the patient exists before creating.
 */
@ApplicationScoped
public class CreateExamRequestUseCase {

    private static final Logger LOG = Logger.getLogger(CreateExamRequestUseCase.class);

    private final ExamRequestRepository examRequestRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final LogAuditEventUseCase auditUseCase;

    @Inject
    public CreateExamRequestUseCase(ExamRequestRepository examRequestRepository,
                                    PatientRepository patientRepository,
                                    NotificationService notificationService,
                                    LogAuditEventUseCase auditUseCase) {
        this.examRequestRepository = examRequestRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
        this.auditUseCase = auditUseCase;
    }

    public Uni<ExamRequest> execute(UUID clinicId, CreateExamRequestRequest request, String requestedBy) {
        return patientRepository.findById(request.patientId())
                .flatMap(patientOpt -> {
                    if (patientOpt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Patient", request.patientId()));
                    }

                    ExamRequest examRequest = ExamRequestMapper.toDomain(clinicId, request, requestedBy);
                    LOG.infof("Creating exam request: id=%s, clinicId=%s, patientId=%s, requestedBy=%s",
                            examRequest.id(), clinicId, request.patientId(), requestedBy);
                    return examRequestRepository.save(examRequest)
                            .call(saved -> notificationService.notifyAllActiveSpecialists(
                                    NotificationType.EXAM_REQUEST_CREATED,
                                    "Nova solicitação de exame",
                                    null, saved.id(), "EXAM_REQUEST"))
                            .flatMap(saved -> auditUseCase.execute(
                                    requestedBy, "EXAM_REQUEST", saved.id(), "CREATE", null, saved.toString())
                                    .onFailure().recoverWithNull()
                                    .replaceWith(saved));
                });
    }
}
