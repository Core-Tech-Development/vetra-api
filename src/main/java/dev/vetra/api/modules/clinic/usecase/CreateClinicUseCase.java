package dev.vetra.api.modules.clinic.usecase;

import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.dto.ClinicMapper;
import dev.vetra.api.modules.clinic.dto.CreateClinicRequest;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.security.KeycloakAdminService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Creates a new clinic with PENDING_APPROVAL status.
 * Validates that no duplicate document (CNPJ) exists.
 * Creates a corresponding user in Keycloak with the CLINIC_ADMIN role.
 */
@ApplicationScoped
public class CreateClinicUseCase {

    private static final Logger LOG = Logger.getLogger(CreateClinicUseCase.class);

    private final ClinicRepository clinicRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Inject
    public CreateClinicUseCase(ClinicRepository clinicRepository, KeycloakAdminService keycloakAdminService) {
        this.clinicRepository = clinicRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public Uni<Clinic> execute(CreateClinicRequest request) {
        return clinicRepository.findByDocument(request.document())
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(
                                new DuplicateException("Clinic", "document", request.document())
                        );
                    }

                    Clinic clinic = ClinicMapper.toDomain(request);
                    LOG.infof("Creating clinic: id=%s, document=%s", clinic.id(), clinic.document());
                    LOG.infof("[DEV-ONLY] Clinic credentials — email: %s | password: %s", request.email(), request.password());

                    return clinicRepository.save(clinic)
                            .flatMap(saved -> keycloakAdminService.createUser(
                                            request.email(),
                                            request.password(),
                                            request.name(),
                                            List.of("CLINIC_ADMIN"))
                                    .onFailure().invoke(err ->
                                            LOG.errorf(err, "Failed to create Keycloak user for clinic %s", saved.id()))
                                    .onFailure().recoverWithItem((String) null)
                                    .map(keycloakUserId -> {
                                        if (keycloakUserId != null) {
                                            LOG.infof("Keycloak user linked to clinic: clinicId=%s, keycloakUserId=%s",
                                                    saved.id(), keycloakUserId);
                                        }
                                        return saved;
                                    }));
                });
    }
}
