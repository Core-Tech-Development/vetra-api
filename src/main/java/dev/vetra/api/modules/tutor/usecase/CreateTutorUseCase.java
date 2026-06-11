package dev.vetra.api.modules.tutor.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.tutor.domain.Tutor;
import dev.vetra.api.modules.tutor.dto.CreateTutorRequest;
import dev.vetra.api.modules.tutor.dto.TutorMapper;
import dev.vetra.api.modules.tutor.repository.TutorRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Creates a new tutor for a given clinic.
 * Validates that the clinic exists and no duplicate document exists within the clinic.
 */
@ApplicationScoped
public class CreateTutorUseCase {

    private static final Logger LOG = Logger.getLogger(CreateTutorUseCase.class);

    private final TutorRepository tutorRepository;
    private final ClinicRepository clinicRepository;

    @Inject
    public CreateTutorUseCase(TutorRepository tutorRepository, ClinicRepository clinicRepository) {
        this.tutorRepository = tutorRepository;
        this.clinicRepository = clinicRepository;
    }

    public Uni<Tutor> execute(UUID clinicId, CreateTutorRequest request) {
        return clinicRepository.findById(clinicId)
                .flatMap(clinicOpt -> {
                    if (clinicOpt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Clinic", clinicId));
                    }

                    var clinic = clinicOpt.get();
                    if (clinic.status() != ClinicStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new BusinessException("CLINIC_NOT_ACTIVE",
                                        "Only clinics with ACTIVE status can manage tutors. Current status: " + clinic.status())
                        );
                    }

                    if (request.document() != null && !request.document().isBlank()) {
                        return tutorRepository.findByDocumentAndClinicId(request.document(), clinicId)
                                .flatMap(existing -> {
                                    if (existing.isPresent()) {
                                        return Uni.createFrom().failure(
                                                new DuplicateException("Tutor", "document", request.document())
                                        );
                                    }
                                    return createAndSave(clinicId, request);
                                });
                    }

                    return createAndSave(clinicId, request);
                });
    }

    private Uni<Tutor> createAndSave(UUID clinicId, CreateTutorRequest request) {
        Tutor tutor = TutorMapper.toDomain(clinicId, request);
        LOG.infof("Creating tutor: id=%s, clinicId=%s", tutor.id(), tutor.clinicId());
        return tutorRepository.save(tutor);
    }
}
