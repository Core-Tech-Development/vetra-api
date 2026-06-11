package dev.vetra.api.modules.laudo.usecase;

import dev.vetra.api.modules.laudo.domain.Laudo;
import dev.vetra.api.modules.laudo.repository.LaudoRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Retrieves a laudo by its unique identifier.
 */
@ApplicationScoped
public class GetLaudoUseCase {

    private static final Logger LOG = Logger.getLogger(GetLaudoUseCase.class);

    private final LaudoRepository laudoRepository;

    @Inject
    public GetLaudoUseCase(LaudoRepository laudoRepository) {
        this.laudoRepository = laudoRepository;
    }

    public Uni<Laudo> execute(UUID id) {
        LOG.debugf("Fetching laudo: id=%s", id);
        return laudoRepository.findById(id)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Laudo", id)));
    }
}
