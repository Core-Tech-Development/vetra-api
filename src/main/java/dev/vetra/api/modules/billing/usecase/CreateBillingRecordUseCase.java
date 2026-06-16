package dev.vetra.api.modules.billing.usecase;

import dev.vetra.api.modules.billing.domain.BillingPayment;
import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.repository.BillingPaymentRepository;
import dev.vetra.api.modules.billing.repository.BillingRecordRepository;
import dev.vetra.api.modules.billing.service.AsaasApiClient;
import dev.vetra.api.modules.billing.service.AsaasCustomerSyncService;
import dev.vetra.api.modules.billing.service.BillingPriceResolver;
import dev.vetra.api.modules.exam.repository.ExamRequestRepository;
import dev.vetra.api.modules.notification.domain.NotificationType;
import dev.vetra.api.modules.notification.service.NotificationService;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ApplicationScoped
public class CreateBillingRecordUseCase {

    private static final Logger LOG = Logger.getLogger(CreateBillingRecordUseCase.class);

    private final BillingRecordRepository billingRecordRepository;
    private final BillingPaymentRepository billingPaymentRepository;
    private final BillingPriceResolver priceResolver;
    private final AsaasCustomerSyncService customerSyncService;
    private final AsaasApiClient asaasApiClient;
    private final AppointmentRepository appointmentRepository;
    private final ExamRequestRepository examRequestRepository;
    private final NotificationService notificationService;

    @ConfigProperty(name = "vetra.asaas.default-billing-type", defaultValue = "PIX")
    String defaultBillingType;

    @ConfigProperty(name = "vetra.asaas.default-due-days", defaultValue = "3")
    int defaultDueDays;

    @Inject
    public CreateBillingRecordUseCase(BillingRecordRepository billingRecordRepository,
                                      BillingPaymentRepository billingPaymentRepository,
                                      BillingPriceResolver priceResolver,
                                      AsaasCustomerSyncService customerSyncService,
                                      AsaasApiClient asaasApiClient,
                                      AppointmentRepository appointmentRepository,
                                      ExamRequestRepository examRequestRepository,
                                      NotificationService notificationService) {
        this.billingRecordRepository = billingRecordRepository;
        this.billingPaymentRepository = billingPaymentRepository;
        this.priceResolver = priceResolver;
        this.customerSyncService = customerSyncService;
        this.asaasApiClient = asaasApiClient;
        this.appointmentRepository = appointmentRepository;
        this.examRequestRepository = examRequestRepository;
        this.notificationService = notificationService;
    }

    public Uni<BillingRecord> execute(UUID laudoId, UUID appointmentId, UUID clinicId, UUID specialistId) {
        LOG.infof("Creating billing record for laudo=%s, appointment=%s", laudoId, appointmentId);

        return appointmentRepository.findById(appointmentId)
                .flatMap(aptOpt -> {
                    if (aptOpt.isEmpty()) {
                        return Uni.createFrom().failure(new RuntimeException("Appointment not found: " + appointmentId));
                    }
                    return examRequestRepository.findById(aptOpt.get().examRequestId());
                })
                .flatMap(erOpt -> {
                    if (erOpt.isEmpty()) {
                        return Uni.createFrom().failure(new RuntimeException("ExamRequest not found"));
                    }
                    String examType = erOpt.get().examType();

                    return priceResolver.resolvePrice(specialistId, examType)
                            .flatMap(price -> {
                                BillingRecord record = BillingRecord.create(
                                        laudoId, appointmentId, clinicId, specialistId,
                                        examType, price.totalCents(), price.platformFeeCents());

                                return billingRecordRepository.save(record)
                                        .flatMap(saved -> attemptAsaasPayment(saved, clinicId))
                                        .call(saved -> notificationService.notifyClinicAdmins(
                                                clinicId,
                                                NotificationType.BILLING_RECORD_CREATED,
                                                "Novo registro de cobrança",
                                                null, saved.id(), "BILLING_RECORD"));
                            });
                });
    }

    private Uni<BillingRecord> attemptAsaasPayment(BillingRecord record, UUID clinicId) {
        return customerSyncService.ensureCustomer(clinicId)
                .flatMap(asaasCustomerId -> {
                    double valueReais = record.totalCents() / 100.0;
                    String dueDate = LocalDate.now().plusDays(defaultDueDays)
                            .format(DateTimeFormatter.ISO_LOCAL_DATE);
                    String description = "Vetra - Laudo " + record.examType();

                    return asaasApiClient.createPayment(
                            asaasCustomerId, defaultBillingType, valueReais,
                            dueDate, description, record.id().toString());
                })
                .flatMap(paymentResult -> {
                    String asaasPaymentId = paymentResult.getString("id");
                    String paymentStatus = paymentResult.getString("status", "PENDING");

                    BillingRecord updated = record.withPaymentCreated(asaasPaymentId);
                    return billingRecordRepository.update(updated)
                            .flatMap(savedRecord ->
                                    fetchPixDetails(asaasPaymentId)
                                            .flatMap(pixInfo -> {
                                                BillingPayment payment = BillingPayment.create(
                                                        savedRecord.id(), asaasPaymentId, paymentStatus,
                                                        defaultBillingType,
                                                        pixInfo.getString("encodedImage"),
                                                        pixInfo.getString("payload"),
                                                        paymentResult.getString("bankSlipUrl"),
                                                        paymentResult.getString("invoiceUrl"),
                                                        LocalDate.now().plusDays(defaultDueDays),
                                                        savedRecord.totalCents());
                                                return billingPaymentRepository.save(payment)
                                                        .map(p -> savedRecord);
                                            })
                            );
                })
                .onFailure().recoverWithUni(err -> {
                    LOG.errorf(err, "Failed to create Asaas payment for billing %s", record.id());
                    BillingRecord withError = record.withError(err.getMessage());
                    return billingRecordRepository.update(withError);
                });
    }

    private Uni<JsonObject> fetchPixDetails(String paymentId) {
        return asaasApiClient.getPixQrCode(paymentId)
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("Could not fetch PIX QR code: %s", err.getMessage());
                    return new JsonObject();
                });
    }
}
