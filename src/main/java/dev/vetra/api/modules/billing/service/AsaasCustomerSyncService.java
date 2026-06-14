package dev.vetra.api.modules.billing.service;

import dev.vetra.api.modules.billing.domain.AsaasCustomer;
import dev.vetra.api.modules.billing.repository.AsaasCustomerRepository;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class AsaasCustomerSyncService {

    private static final Logger LOG = Logger.getLogger(AsaasCustomerSyncService.class);

    private final AsaasCustomerRepository asaasCustomerRepository;
    private final ClinicRepository clinicRepository;
    private final AsaasApiClient asaasApiClient;

    @Inject
    public AsaasCustomerSyncService(AsaasCustomerRepository asaasCustomerRepository,
                                     ClinicRepository clinicRepository,
                                     AsaasApiClient asaasApiClient) {
        this.asaasCustomerRepository = asaasCustomerRepository;
        this.clinicRepository = clinicRepository;
        this.asaasApiClient = asaasApiClient;
    }

    public Uni<String> ensureCustomer(UUID clinicId) {
        return asaasCustomerRepository.findByClinicId(clinicId)
                .flatMap(opt -> {
                    if (opt.isPresent()) {
                        return Uni.createFrom().item(opt.get().asaasCustomerId());
                    }
                    return createAsaasCustomer(clinicId);
                });
    }

    private Uni<String> createAsaasCustomer(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new RuntimeException("Clinic not found: " + clinicId));
                    }
                    var clinic = opt.get();
                    return asaasApiClient.createCustomer(
                                    clinic.name(), clinic.document(), clinic.email(), clinicId.toString())
                            .flatMap(result -> {
                                String asaasId = result.getString("id");
                                AsaasCustomer customer = AsaasCustomer.create(clinicId, asaasId);
                                return asaasCustomerRepository.save(customer)
                                        .map(saved -> {
                                            LOG.infof("Asaas customer synced for clinic %s: %s", clinicId, asaasId);
                                            return saved.asaasCustomerId();
                                        });
                            });
                });
    }
}
