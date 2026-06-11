package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.domain.ClinicStatus;
import dev.vetra.api.modules.clinic.dto.CreateClinicStaffRequest;
import dev.vetra.api.modules.clinic.dto.ClinicStaffMapper;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.exception.NotFoundException;
import dev.vetra.api.shared.security.KeycloakAdminService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Creates a new clinic staff member (veterinarian or secretary) for a given clinic.
 * Validates that the clinic exists and is ACTIVE, and that no duplicate email exists.
 * Also creates a corresponding Keycloak user.
 */
@ApplicationScoped
public class CreateClinicStaffUseCase {

    private static final Logger LOG = Logger.getLogger(CreateClinicStaffUseCase.class);

    private final ClinicRepository clinicRepository;
    private final ClinicStaffRepository clinicStaffRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Inject
    public CreateClinicStaffUseCase(ClinicRepository clinicRepository,
                                    ClinicStaffRepository clinicStaffRepository,
                                    KeycloakAdminService keycloakAdminService) {
        this.clinicRepository = clinicRepository;
        this.clinicStaffRepository = clinicStaffRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public Uni<ClinicStaff> execute(UUID clinicId, CreateClinicStaffRequest request) {
        return clinicRepository.findById(clinicId)
                .flatMap(clinicOpt -> {
                    if (clinicOpt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("Clinic", clinicId));
                    }

                    var clinic = clinicOpt.get();
                    if (clinic.status() != ClinicStatus.ACTIVE) {
                        return Uni.createFrom().failure(
                                new BusinessException("CLINIC_NOT_ACTIVE",
                                        "Only clinics with ACTIVE status can manage staff. Current status: " + clinic.status())
                        );
                    }

                    return clinicStaffRepository.findByEmail(request.email())
                            .flatMap(existing -> {
                                if (existing.isPresent()) {
                                    return Uni.createFrom().failure(
                                            new DuplicateException("ClinicStaff", "email", request.email())
                                    );
                                }
                                return createAndSave(clinicId, request);
                            });
                });
    }

    private Uni<ClinicStaff> createAndSave(UUID clinicId, CreateClinicStaffRequest request) {
        ClinicStaff staff = ClinicStaffMapper.toDomain(clinicId, request);
        LOG.infof("Creating clinic staff: id=%s, clinicId=%s, role=%s", staff.id(), staff.clinicId(), staff.role());

        return clinicStaffRepository.save(staff)
                .flatMap(savedStaff ->
                        keycloakAdminService.createUser(request.email(), request.password(), request.name(), List.of("CLINIC_STAFF"))
                                .flatMap(keycloakUserId ->
                                        clinicStaffRepository.updateUserId(savedStaff.id(), keycloakUserId)
                                                .map(v -> {
                                                    LOG.infof("[DEV-ONLY] Staff credentials: email=%s, password=%s", request.email(), request.password());
                                                    return savedStaff;
                                                })
                                )
                                .onFailure().recoverWithUni(error -> {
                                    LOG.errorf(error, "Failed to create Keycloak user for staff: email=%s", request.email());
                                    return Uni.createFrom().item(savedStaff);
                                })
                );
    }
}
