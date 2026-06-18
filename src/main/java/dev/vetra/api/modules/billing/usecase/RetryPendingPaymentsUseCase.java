package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingPayment;
import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.domain.BillingRecordStatus;
import dev.vetra.api.modules.billing.repository.BillingPaymentRepository;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.modules.billing.service.AsaasApiClient;
import dev.vetra.api.modules.billing.service.AsaasCustomerSyncService;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class RetryPendingPaymentsUseCase {

    private static final Logger LOG = Logger.getLogger(RetryPendingPaymentsUseCase.class);
    private static final Duration MAX_RETRY_AGE = Duration.ofHours(24);

    private final BillingRecordRepository billingRecordRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final AsaasCustomerSyncService customerSyncService;
    private final AsaasApiClient asaasApiClient;

    @ConfigProperty(name = "vetra.asaas.enabled", defaultValue = "false")
    boolean asaasEnabled;

    @ConfigProperty(name = "vetra.asaas.default-billing-type", defaultValue = "PIX")
    String defaultBillingType;

    @ConfigProperty(name = "vetra.asaas.default-due-days", defaultValue = "3")
    int defaultDueDays;

    @Inject
    public RetryPendingPaymentsUseCase(BillingRecordRepository billingRecordRepository,
                                        BillingPaymentRepository billingPaymentRepository,
                                        AsaasCustomerSyncService customerSyncService,
                                        AsaasApiClient asaasApiClient) {
        this.billingRecordRepository = billingRecordRepository;
        this.billingPaymentRepository = billingPaymentRepository;
        this.customerSyncService = customerSyncService;
        this.asaasApiClient = asaasApiClient;
    }

    @Scheduled(every = "5m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public Uni<Void> retryPending() {
        if (!asaasEnabled) {
            return Uni.createFrom().voidItem();
        }

        Instant cutoff = Instant.now().minus(MAX_RETRY_AGE);

        return billingRecordRepository.findByStatus(BillingRecordStatus.PENDING_PAYMENT_CREATION)
                .flatMap(records -> {
                    if (records.isEmpty()) return Uni.createFrom().voidItem();
                    LOG.infof("Retrying %d pending billing records", records.size());

                    return Multi.createFrom().iterable(records)
                            .onItem().transformToUni(record -> {
                                if (record.createdAt().isBefore(cutoff)) {
                                    LOG.warnf("Billing %s exceeded retry window, marking FAILED", record.id());
                                    return billingRecordRepository.update(
                                            record.withStatus(BillingRecordStatus.FAILED)
                                                    .withError("Exceeded 24h retry window"))
                                            .replaceWithVoid();
                                }
                                return retryPayment(record)
                                        .onFailure().recoverWithNull()
                                        .replaceWithVoid();
                            }).merge(10)
                            .collect().asList()
                            .replaceWithVoid();
                });
    }

    private Uni<BillingRecord> retryPayment(BillingRecord record) {
        return customerSyncService.ensureCustomer(record.clinicId())
                .flatMap(customerId -> {
                    double valueReais = record.totalCents() / 100.0;
                    String dueDate = LocalDate.now().plusDays(defaultDueDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
                    return asaasApiClient.createPayment(customerId, defaultBillingType, valueReais,
                            dueDate, "Vetra - Laudo " + record.examType(), record.id().toString());
                })
                .flatMap(paymentResult -> {
                    String asaasPaymentId = paymentResult.getString("id");
                    BillingRecord updated = record.withPaymentCreated(asaasPaymentId);
                    return billingRecordRepository.update(updated)
                            .flatMap(saved -> {
                                BillingPayment payment = BillingPayment.create(
                                        saved.id(), asaasPaymentId,
                                        paymentResult.getString("status", "PENDING"),
                                        defaultBillingType, null, null,
                                        paymentResult.getString("bankSlipUrl"),
                                        paymentResult.getString("invoiceUrl"),
                                        LocalDate.now().plusDays(defaultDueDays),
                                        saved.totalCents());
                                return billingPaymentRepository.save(payment).map(p -> saved);
                            });
                })
                .onItem().invoke(r -> LOG.infof("Retry succeeded for billing %s", record.id()))
                .onFailure().invoke(err -> LOG.warnf(err, "Retry failed for billing %s", record.id()));
    }
}
