package dev.vetra.api.modules.clinic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.modules.clinic.domain.Clinic;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CachedClinicService {

    private static final TypeReference<Optional<Clinic>> CLINIC_OPT_REF = new TypeReference<>() {};

    private final ClinicRepository repository;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @Inject
    public CachedClinicService(ClinicRepository repository,
                                ReactiveCacheService cache,
                                CacheTtl ttl) {
        this.repository = repository;
        this.cache = cache;
        this.ttl = ttl;
    }

    public Uni<Optional<Clinic>> findById(UUID id) {
        return cache.getOrLoad(
                CacheKeys.clinic(id),
                ttl.clinicProfile(),
                CLINIC_OPT_REF,
                repository.findById(id));
    }

    public Uni<Void> invalidate(UUID id) {
        return cache.invalidate(CacheKeys.clinic(id));
    }
}
