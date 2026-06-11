package dev.vetra.api.modules.admin.usecase;

import dev.vetra.api.modules.admin.dto.AdminDashboardResponse;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.specialist.repository.SpecialistRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Gathers platform-wide statistics for the admin dashboard.
 * Uses Uni.combine() to run count queries in parallel.
 */
@ApplicationScoped
public class GetAdminDashboardUseCase {

    private static final Logger LOG = Logger.getLogger(GetAdminDashboardUseCase.class);

    private final ClinicRepository clinicRepository;
    private final SpecialistRepository specialistRepository;

    @Inject
    public GetAdminDashboardUseCase(ClinicRepository clinicRepository,
                                    SpecialistRepository specialistRepository) {
        this.clinicRepository = clinicRepository;
        this.specialistRepository = specialistRepository;
    }

    public Uni<AdminDashboardResponse> execute() {
        LOG.info("Fetching admin dashboard statistics");

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
