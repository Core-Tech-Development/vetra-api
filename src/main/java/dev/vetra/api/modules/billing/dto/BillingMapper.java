package dev.vetra.api.modules.billing.dto;

import dev.vetra.api.modules.billing.domain.BillingPayment;
import dev.vetra.api.modules.billing.domain.BillingRecord;
import dev.vetra.api.modules.billing.domain.ExamTypePricing;
import dev.vetra.api.modules.billing.domain.SpecialistPricing;

public final class BillingMapper {

    private BillingMapper() {}

    public static BillingRecordResponse toResponse(BillingRecord r) {
        return new BillingRecordResponse(
                r.id(), r.laudoId(), r.appointmentId(), r.clinicId(), r.specialistId(),
                r.examType(), r.totalCents(), r.platformFeeCents(), r.specialistShareCents(),
                r.status().name(), r.asaasPaymentId(), r.errorMessage(),
                r.createdAt(), r.updatedAt());
    }

    public static BillingPaymentResponse toResponse(BillingPayment p) {
        return new BillingPaymentResponse(
                p.id(), p.billingRecordId(), p.asaasPaymentId(), p.status(),
                p.billingType(), p.pixQrCode(), p.pixCopyPaste(), p.boletoUrl(),
                p.invoiceUrl(), p.dueDate(), p.paidAt(), p.valueCents(),
                p.netValueCents(), p.createdAt(), p.updatedAt());
    }

    public static ExamTypePricingResponse toResponse(ExamTypePricing p) {
        return new ExamTypePricingResponse(
                p.id(), p.examType(), p.priceCents(), p.platformFeePercent(),
                p.active(), p.createdAt(), p.updatedAt());
    }

    public static SpecialistPricingResponse toResponse(SpecialistPricing p) {
        return new SpecialistPricingResponse(
                p.id(), p.specialistId(), p.examType(), p.priceCents(),
                p.active(), p.createdAt(), p.updatedAt());
    }
}
