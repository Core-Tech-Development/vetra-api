package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.domain.BillingRecordStatus;
import dev.vetra.api.modules.billing.domain.WebhookEventLog;
import dev.vetra.api.modules.billing.repository.BillingPaymentRepository;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.modules.billing.repository.WebhookEventLogRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;

@ApplicationScoped
public class ProcessAsaasWebhookUseCase {

    private static final Logger LOG = Logger.getLogger(ProcessAsaasWebhookUseCase.class);

    private static final Map<String, BillingRecordStatus> EVENT_STATUS_MAP = Map.of(
            "PAYMENT_CREATED", BillingRecordStatus.PAYMENT_CREATED,
            "PAYMENT_CONFIRMED", BillingRecordStatus.PAYMENT_CONFIRMED,
            "PAYMENT_RECEIVED", BillingRecordStatus.PAYMENT_RECEIVED,
            "PAYMENT_OVERDUE", BillingRecordStatus.PAYMENT_OVERDUE,
            "PAYMENT_REFUNDED", BillingRecordStatus.PAYMENT_REFUNDED,
            "PAYMENT_DELETED", BillingRecordStatus.FAILED
    );

    private final WebhookEventLogRepository webhookEventLogRepository;
    private final BillingRecordRepository billingRecordRepository;
    private final BillingPaymentRepository billingPaymentRepository;

    @Inject
    public ProcessAsaasWebhookUseCase(WebhookEventLogRepository webhookEventLogRepository,
                                       BillingRecordRepository billingRecordRepository,
                                       BillingPaymentRepository billingPaymentRepository) {
        this.webhookEventLogRepository = webhookEventLogRepository;
        this.billingRecordRepository = billingRecordRepository;
        this.billingPaymentRepository = billingPaymentRepository;
    }

    public Uni<Void> execute(String eventId, String eventType, String asaasPaymentId, String rawPayload) {
        LOG.infof("Processing webhook: event=%s, type=%s, payment=%s", eventId, eventType, asaasPaymentId);

        WebhookEventLog log = WebhookEventLog.create(eventId, eventType, asaasPaymentId, rawPayload);

        return webhookEventLogRepository.save(log)
                .flatMap(savedLog -> {
                    if (eventId != null) {
                        return webhookEventLogRepository.findByEventId(eventId)
                                .flatMap(existing -> processEvent(savedLog, eventType, asaasPaymentId));
                    }
                    return processEvent(savedLog, eventType, asaasPaymentId);
                })
                .onFailure().invoke(err ->
                        LOG.errorf(err, "Failed to process webhook event: %s", eventId));
    }

    private Uni<Void> processEvent(WebhookEventLog log, String eventType, String asaasPaymentId) {
        BillingRecordStatus newStatus = EVENT_STATUS_MAP.get(eventType);
        if (newStatus == null) {
            LOG.infof("Ignoring unhandled event type: %s", eventType);
            return webhookEventLogRepository.markProcessed(log.id());
        }

        if (asaasPaymentId == null || asaasPaymentId.isBlank()) {
            LOG.warnf("Webhook event %s has no payment ID", eventType);
            return webhookEventLogRepository.markProcessed(log.id());
        }

        return billingRecordRepository.findByAsaasPaymentId(asaasPaymentId)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        LOG.warnf("No billing record found for Asaas payment: %s", asaasPaymentId);
                        return webhookEventLogRepository.markProcessed(log.id());
                    }

                    BillingRecord record = opt.get();
                    BillingRecord updated = record.withStatus(newStatus);
                    return billingRecordRepository.update(updated)
                            .flatMap(savedRecord ->
                                    billingPaymentRepository.findByAsaasPaymentId(asaasPaymentId)
                                            .flatMap(paymentOpt -> {
                                                if (paymentOpt.isPresent()) {
                                                    var payment = paymentOpt.get();
                                                    var updatedPayment = payment.withStatus(newStatus.name());
                                                    if (newStatus == BillingRecordStatus.PAYMENT_RECEIVED) {
                                                        updatedPayment = updatedPayment.withPaid(Instant.now(), null);
                                                    }
                                                    return billingPaymentRepository.update(updatedPayment)
                                                            .replaceWithVoid();
                                                }
                                                return Uni.createFrom().voidItem();
                                            })
                            )
                            .flatMap(v -> webhookEventLogRepository.markProcessed(log.id()))
                            .invoke(() -> LOG.infof("Billing record %s updated to %s", record.id(), newStatus));
                });
    }
}
