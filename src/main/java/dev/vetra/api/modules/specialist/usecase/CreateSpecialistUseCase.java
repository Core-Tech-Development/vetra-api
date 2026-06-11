package dev.vetra.api.modules.specialist.usecase;

import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.dto.CreateSpecialistRequest;
import dev.vetra.api.modules.specialist.dto.SpecialistMapper;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.exception.DuplicateException;
import dev.vetra.api.shared.security.KeycloakAdminService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Creates a new specialist with PENDING_APPROVAL status.
 * Validates that no duplicate CRMV + CRMV state combination exists.
 * Creates a corresponding user in Keycloak with the SPECIALIST role.
 */
@ApplicationScoped
public class CreateSpecialistUseCase {

    private static final Logger LOG = Logger.getLogger(CreateSpecialistUseCase.class);

    private final SpecialistRepository specialistRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Inject
    public CreateSpecialistUseCase(SpecialistRepository specialistRepository, KeycloakAdminService keycloakAdminService) {
        this.specialistRepository = specialistRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    public Uni<Specialist> execute(CreateSpecialistRequest request, String userId) {
        return specialistRepository.findByCrmv(request.crmv(), request.crmvState())
                .flatMap(existing -> {
                    if (existing.isPresent()) {
                        return Uni.createFrom().failure(
                                new DuplicateException("Specialist", "crmv", request.crmv() + "/" + request.crmvState())
                        );
                    }

                    Specialist specialist = SpecialistMapper.toDomain(request, userId);
                    LOG.infof("Creating specialist: id=%s, crmv=%s/%s", specialist.id(), specialist.crmv(), specialist.crmvState());
                    LOG.infof("[DEV-ONLY] Specialist credentials — email: %s | password: %s", request.email(), request.password());

                    return specialistRepository.save(specialist)
                            .flatMap(saved -> keycloakAdminService.createUser(
                                            request.email(),
                                            request.password(),
                                            request.name(),
                                            List.of("SPECIALIST"))
                                    .onFailure().invoke(err ->
                                            LOG.errorf(err, "Failed to create Keycloak user for specialist %s", saved.id()))
                                    .onFailure().recoverWithItem((String) null)
                                    .flatMap(keycloakUserId -> {
                                        if (keycloakUserId != null) {
                                            LOG.infof("Keycloak user linked to specialist: specialistId=%s, keycloakUserId=%s",
                                                    saved.id(), keycloakUserId);
                                            return specialistRepository.updateUserId(saved.id(), keycloakUserId)
                                                    .replaceWith(saved);
                                        }
                                        return Uni.createFrom().item(saved);
                                    }));
                });
    }
}
