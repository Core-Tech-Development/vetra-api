package dev.vetra.api.modules.billing.service;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import dev.vetra.api.modules.billing.domain.SpecialistPricing;
import dev.vetra.api.modules.billing.repository.ExamTypePricingRepository;
import dev.vetra.api.modules.billing.repository.SpecialistPricingRepository;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CachedPricingService {

    private static final TypeReference<Optional<ExamTypePricing>> EXAM_TYPE_OPT_REF = new TypeReference<>() {};
    private static final TypeReference<Optional<SpecialistPricing>> SPECIALIST_OPT_REF = new TypeReference<>() {};
    private static final TypeReference<List<ExamTypePricing>> EXAM_TYPE_LIST_REF = new TypeReference<>() {};

    private final ExamTypePricingRepository examTypePricingRepo;
    private final SpecialistPricingRepository specialistPricingRepo;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @Inject
    public CachedPricingService(ExamTypePricingRepository examTypePricingRepo,
                                 SpecialistPricingRepository specialistPricingRepo,
                                 ReactiveCacheService cache,
                                 CacheTtl ttl) {
        this.examTypePricingRepo = examTypePricingRepo;
        this.specialistPricingRepo = specialistPricingRepo;
        this.cache = cache;
        this.ttl = ttl;
    }

    public Uni<Optional<ExamTypePricing>> findByExamType(String examType) {
        return cache.getOrLoad(
                CacheKeys.examTypePricing(examType),
                ttl.examTypePricing(),
                EXAM_TYPE_OPT_REF,
                examTypePricingRepo.findByExamType(examType));
    }

    public Uni<Optional<SpecialistPricing>> findBySpecialistIdAndExamType(UUID specialistId, String examType) {
        return cache.getOrLoad(
                CacheKeys.specialistPricing(specialistId, examType),
                ttl.specialistPricing(),
                SPECIALIST_OPT_REF,
                specialistPricingRepo.findBySpecialistIdAndExamType(specialistId, examType));
    }

    public Uni<List<ExamTypePricing>> findAllActive() {
        return cache.getOrLoad(
                CacheKeys.examTypePricingAllActive(),
                ttl.examTypePricing(),
                EXAM_TYPE_LIST_REF,
                examTypePricingRepo.findAllActive());
    }

    public Uni<Void> invalidateExamTypePricing() {
        return cache.invalidateByPattern(CacheKeys.allExamTypePricing());
    }

    public Uni<Void> invalidateSpecialistPricing(UUID specialistId) {
        return cache.invalidateByPattern(CacheKeys.allSpecialistPricing(specialistId));
    }
}
