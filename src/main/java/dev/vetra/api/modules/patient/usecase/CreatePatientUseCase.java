package dev.vetra.api.modules.patient.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.patient.domain.Patient;
import dev.vetra.api.modules.patient.dto.CreatePatientRequest;
import dev.vetra.api.modules.patient.dto.PatientMapper;
import dev.vetra.api.modules.patient.repository.PatientRepository;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Creates a new patient for a given tutor and clinic.
 * Validates that the tutor exists before creating the patient.
 */
@ApplicationScoped
public class CreatePatientUseCase {

    private static final Logger LOG = Logger.getLogger(CreatePatientUseCase.class);

    private final PatientRepository patientRepository;
    private final TutorRepository tutorRepository;
    private final ClinicRepository clinicRepository;

    @Inject
    public CreatePatientUseCase(PatientRepository patientRepository, TutorRepository tutorRepository,
                                ClinicRepository clinicRepository) {
        this.patientRepository = patientRepository;
        this.tutorRepository = tutorRepository;
        this.clinicRepository = clinicRepository;
    }

    public Uni<Patient> execute(UUID clinicId, UUID tutorId, CreatePatientRequest request) {
        return clinicRepository.findById(clinicId)
                .flatMap(clinicOpt -> {
                    if (clinicOpt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Clinic", clinicId));
                    }

                    var clinic = clinicOpt.get();
                    if (clinic.status() != ClinicStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new BusinessException("CLINIC_NOT_ACTIVE",
                                        "Only clinics with ACTIVE status can manage patients. Current status: " + clinic.status())
                        );
                    }

                    return tutorRepository.findById(tutorId)
                            .flatMap(tutorOpt -> {
                                if (tutorOpt.isEmpty()) {
                                    return Uni.createFrom().failure(new NotFoundException("Tutor", tutorId));
                                }

                                Patient patient = PatientMapper.toDomain(clinicId, tutorId, request);
                                LOG.infof("Creating patient: id=%s, clinicId=%s, tutorId=%s", patient.id(), clinicId, tutorId);
                                return patientRepository.save(patient);
                            });
                });
    }
}
