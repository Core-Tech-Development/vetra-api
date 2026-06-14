package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import dev.vetra.api.modules.billing.repository.ExamTypePricingRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ListExamTypePricingsUseCase {

    private final ExamTypePricingRepository repository;

    @Inject
    public ListExamTypePricingsUseCase(ExamTypePricingRepository repository) {
        this.repository = repository;
    }

    public Uni<List<ExamTypePricing>> execute() {
        return repository.findAll();
    }
}
