package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.SpecialistPricing;
import dev.vetra.api.modules.billing.repository.SpecialistPricingRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListSpecialistPricingsUseCase {

    private final SpecialistPricingRepository repository;

    @Inject
    public ListSpecialistPricingsUseCase(SpecialistPricingRepository repository) {
        this.repository = repository;
    }

    public Uni<List<SpecialistPricing>> execute(UUID specialistId) {
        return repository.findBySpecialistId(specialistId);
    }
}
