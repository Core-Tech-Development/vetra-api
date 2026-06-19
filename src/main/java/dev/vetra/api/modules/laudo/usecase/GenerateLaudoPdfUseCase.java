package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.imaging.service.MinioStorageService;
import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.domain.LaudoStatus;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.modules.laudo.service.LaudoPdfService;
import dev.vetra.api.modules.laudo.service.LaudoPdfService.LaudoPdfData;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates a PDF for an issued laudo, uploads it to MinIO, and updates
 * the laudo record with the storage key.
 */
@ApplicationScoped
public class GenerateLaudoPdfUseCase {

    private static final Logger LOG = Logger.getLogger(GenerateLaudoPdfUseCase.class);

    private final LaudoRepository laudoRepository;
    private final AppointmentRepository appointmentRepository;
    private final ExamRequestRepository examRequestRepository;
    private final PatientRepository patientRepository;
    private final SpecialistRepository specialistRepository;
    private final ClinicRepository clinicRepository;
    private final LaudoPdfService pdfService;
    private final MinioStorageService storageService;

    @Inject
    public GenerateLaudoPdfUseCase(LaudoRepository laudoRepository,
                                    AppointmentRepository appointmentRepository,
                                    ExamRequestRepository examRequestRepository,
                                    PatientRepository patientRepository,
                                    SpecialistRepository specialistRepository,
                                    ClinicRepository clinicRepository,
                                    LaudoPdfService pdfService,
                                    MinioStorageService storageService) {
        this.laudoRepository = laudoRepository;
        this.appointmentRepository = appointmentRepository;
        this.examRequestRepository = examRequestRepository;
        this.patientRepository = patientRepository;
        this.specialistRepository = specialistRepository;
        this.clinicRepository = clinicRepository;
        this.pdfService = pdfService;
        this.storageService = storageService;
    }

    public Uni<Laudo> execute(UUID laudoId) {
        LOG.infof("Generating PDF for laudo: id=%s", laudoId);

        return laudoRepository.findById(laudoId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Laudo", laudoId));
                    }
                    Laudo laudo = opt.get();

                    if (laudo.status() != LaudoStatus.ISSUED) {
                        return Uni.createFrom().failure(
                                new BusinessException("INVALID_STATUS",
                                        "PDF can only be generated for ISSUED laudos. Current status: " + laudo.status()));
                    }

                    // Idempotent: if PDF was already generated, return existing laudo
                    if (laudo.pdfStorageKey() != null) {
                        LOG.infof("PDF already generated for laudo %s, returning existing", laudoId);
                        return Uni.createFrom().item(laudo);
                    }

                    // Load related entities in parallel
                    var appointmentUni = appointmentRepository.findById(laudo.appointmentId())
                            .map(a -> a.orElseThrow(() ->
                                    new NotFoundException("Appointment", laudo.appointmentId())));

                    var specialistUni = specialistRepository.findById(laudo.specialistId())
                            .map(s -> s.orElseThrow(() ->
                                    new NotFoundException("Specialist", laudo.specialistId())));

                    return Uni.combine().all().unis(appointmentUni, specialistUni)
                            .asTuple()
                            .flatMap(tuple -> {
                                var appointment = tuple.getItem1();
                                var specialist = tuple.getItem2();

                                return examRequestRepository.findById(appointment.examRequestId())
                                        .flatMap(erOpt -> {
                                            var examRequest = erOpt.orElseThrow(() ->
                                                    new NotFoundException("ExamRequest", appointment.examRequestId()));

                                            // Load patient and clinic in parallel
                                            var patientUni = patientRepository.findById(examRequest.patientId());
                                            var clinicUni = clinicRepository.findById(examRequest.clinicId());

                                            return Uni.combine().all().unis(patientUni, clinicUni)
                                                    .asTuple()
                                                    .flatMap(tuple2 -> {
                                                        var patient = tuple2.getItem1().orElse(null);
                                                        var clinic = tuple2.getItem2().orElse(null);

                                                        LaudoPdfData pdfData = new LaudoPdfData(
                                                                laudo.id().toString(),
                                                                laudo.findings(),
                                                                laudo.conclusion(),
                                                                laudo.recommendations(),
                                                                laudo.issuedAt(),
                                                                specialist.name(),
                                                                specialist.crmv(),
                                                                specialist.crmvState(),
                                                                specialist.specialty() != null ? specialist.specialty().name() : null,
                                                                patient != null ? patient.name() : null,
                                                                patient != null ? patient.species() : null,
                                                                patient != null ? patient.breed() : null,
                                                                examRequest.examType(),
                                                                clinic != null ? clinic.name() : null,
                                                                examRequest.diagnosticHypothesis()
                                                        );

                                                        String storageKey = "laudos/" + laudoId + "/laudo-" + laudoId + ".pdf";

                                                        // Generate PDF + upload to MinIO (blocking, on worker pool)
                                                        return Uni.createFrom().item(() -> {
                                                                    byte[] pdfBytes = pdfService.generatePdf(pdfData);
                                                                    try {
                                                                        storageService.uploadFile(
                                                                                storageKey,
                                                                                new ByteArrayInputStream(pdfBytes),
                                                                                pdfBytes.length,
                                                                                "application/pdf"
                                                                        );
                                                                    } catch (Exception e) {
                                                                        throw new RuntimeException("Failed to upload laudo PDF to storage", e);
                                                                    }
                                                                    LOG.infof("PDF uploaded to MinIO: key=%s, size=%d bytes", storageKey, pdfBytes.length);
                                                                    return storageKey;
                                                                }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                                                                .flatMap(key -> {
                                                                    // Update laudo with pdfStorageKey
                                                                    Laudo updated = Laudo.restore(
                                                                            laudo.id(),
                                                                            laudo.appointmentId(),
                                                                            laudo.specialistId(),
                                                                            laudo.status(),
                                                                            laudo.findings(),
                                                                            laudo.conclusion(),
                                                                            laudo.recommendations(),
                                                                            key,
                                                                            laudo.issuedAt(),
                                                                            laudo.createdAt(),
                                                                            Instant.now()
                                                                    );
                                                                    return laudoRepository.update(updated);
                                                                });
                                                    });
                                        });
                            });
                });
    }
}
