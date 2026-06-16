package dev.vetra.api.modules.admin.usecase;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.modules.admin.dto.AdminDashboardResponse;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Gathers platform-wide statistics for the admin dashboard.
 * Uses Uni.combine() to run count queries in parallel.
 * Results are cached in Redis with a short TTL.
 */
@ApplicationScoped
public class GetAdminDashboardUseCase {

    private static final Logger LOG = Logger.getLogger(GetAdminDashboardUseCase.class);
    private static final TypeReference<AdminDashboardResponse> DASHBOARD_REF = new TypeReference<>() {};

    private final ClinicRepository clinicRepository;
    private final SpecialistRepository specialistRepository;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @Inject
    public GetAdminDashboardUseCase(ClinicRepository clinicRepository,
                                    SpecialistRepository specialistRepository,
                                    ReactiveCacheService cache,
                                    CacheTtl ttl) {
        this.clinicRepository = clinicRepository;
        this.specialistRepository = specialistRepository;
        this.cache = cache;
        this.ttl = ttl;
    }

    public Uni<AdminDashboardResponse> execute() {
        return cache.getOrLoad(
                CacheKeys.adminDashboard(),
                ttl.dashboard(),
                DASHBOARD_REF,
                loadFromDatabase());
    }

    private Uni<AdminDashboardResponse> loadFromDatabase() {
        LOG.info("Fetching admin dashboard statistics from database");

        Uni<Long> totalClinics = clinicRepository.count();
        Uni<Long> totalSpecialists = specialistRepository.count();

        return Uni.combine().all().unis(totalClinics, totalSpecialists)
                .asTuple()
                .map(tuple -> new AdminDashboardResponse(
                        tuple.getItem1(),
                        tuple.getItem2(),
                        0,
                        0,
                        0,
                        0
                ));
    }
}
