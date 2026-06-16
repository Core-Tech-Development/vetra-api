package dev.vetra.api.modules.specialist.service;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.modules.specialist.domain.Specialist;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CachedSpecialistService {

    private static final TypeReference<Optional<Specialist>> SPECIALIST_OPT_REF = new TypeReference<>() {};

    private final SpecialistRepository repository;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @Inject
    public CachedSpecialistService(SpecialistRepository repository,
                                    ReactiveCacheService cache,
                                    CacheTtl ttl) {
        this.repository = repository;
        this.cache = cache;
        this.ttl = ttl;
    }

    public Uni<Optional<Specialist>> findById(UUID id) {
        return cache.getOrLoad(
                CacheKeys.specialist(id),
                ttl.specialistProfile(),
                SPECIALIST_OPT_REF,
                repository.findById(id));
    }

    public Uni<Void> invalidate(UUID id) {
        return cache.invalidate(CacheKeys.specialist(id));
    }
}
