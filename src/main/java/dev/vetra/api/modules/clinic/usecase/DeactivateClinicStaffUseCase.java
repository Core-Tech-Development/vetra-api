package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.ClinicStaff;
import dev.vetra.api.modules.clinic.domain.ClinicStaffStatus;
import dev.vetra.api.modules.clinic.repository.ClinicStaffRepository;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.exception.NotFoundException;
import dev.vetra.api.shared.security.KeycloakAdminService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Deactivates a clinic staff member (soft delete).
 * Sets status to INACTIVE and disables the corresponding Keycloak user.
 */
@ApplicationScoped
public class DeactivateClinicStaffUseCase {

    private static final Logger LOG = Logger.getLogger(DeactivateClinicStaffUseCase.class);

    private final ClinicStaffRepository clinicStaffRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Inject
    public DeactivateClinicStaffUseCase(ClinicStaffRepository clinicStaffRepository,
                                        KeycloakAdminService keycloakAdminService) {
        this.clinicStaffRepository = clinicStaffRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public Uni<ClinicStaff> execute(UUID id) {
        return clinicStaffRepository.findById(id)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new NotFoundException("ClinicStaff", id));
                    }

                    ClinicStaff existing = opt.get();
                    if (existing.status() == ClinicStaffStatus.INACTIVE) {
                        return Uni.createFrom().failure(
                                new BusinessException("STAFF_ALREADY_INACTIVE",
                                        "Clinic staff is already inactive: id=" + id)
                        );
                    }

                    ClinicStaff deactivated = ClinicStaff.restore(
                            existing.id(),
                            existing.clinicId(),
                            existing.userId(),
                            existing.name(),
                            existing.email(),
                            existing.phone(),
                            existing.role(),
                            ClinicStaffStatus.INACTIVE,
                            existing.createdAt(),
                            Instant.now()
                    );

                    return clinicStaffRepository.update(deactivated)
                            .flatMap(updated -> {
                                if (updated.userId() != null) {
                                    return keycloakAdminService.disableUser(updated.userId())
                                            .onFailure().recoverWithUni(error -> {
                                                LOG.errorf(error, "Failed to disable Keycloak user for staff: userId=%s", updated.userId());
                                                return Uni.createFrom().voidItem();
                                            })
                                            .map(v -> updated);
                                }
                                return Uni.createFrom().item(updated);
                            });
                });
    }
}
