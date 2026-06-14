package dev.vetra.api.modules.billing.resource;

import dev.vetra.api.modules.billing.dto.BillingMapper;
import dev.vetra.api.modules.billing.dto.BillingRecordResponse;
import dev.vetra.api.modules.billing.usecase.GetBillingRecordUseCase;
import dev.vetra.api.modules.billing.usecase.ListClinicBillingRecordsUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/billing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
@Tag(name = "Billing", description = "Clinic billing records")
public class BillingResource {

    private final ListClinicBillingRecordsUseCase listClinicBillingUseCase;
    private final GetBillingRecordUseCase getBillingRecordUseCase;
    private final SecurityContext securityContext;

    @Inject
    public BillingResource(ListClinicBillingRecordsUseCase listClinicBillingUseCase,
                            GetBillingRecordUseCase getBillingRecordUseCase,
                            SecurityContext securityContext) {
        this.listClinicBillingUseCase = listClinicBillingUseCase;
        this.getBillingRecordUseCase = getBillingRecordUseCase;
        this.securityContext = securityContext;
    }

    @GET
    @Path("/records")
    @Operation(summary = "List billing records for the current clinic")
    public Uni<PageResponse<BillingRecordResponse>> listRecords(
            @QueryParam("clinicId") UUID clinicId,
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);

        if (clinicId == null) {
            return Uni.createFrom().item(PageResponse.of(List.of(), 0, pageRequest));
        }

        Uni<List<BillingRecordResponse>> items = listClinicBillingUseCase.execute(clinicId, pageRequest.page(), pageRequest.size())
                .map(list -> list.stream().map(BillingMapper::toResponse).toList());
        Uni<Long> total = listClinicBillingUseCase.count(clinicId);

        return Uni.combine().all().unis(items, total)
                .asTuple()
                .map(t -> PageResponse.of(t.getItem1(), t.getItem2(), pageRequest));
    }

    @GET
    @Path("/records/{id}")
    @Operation(summary = "Get billing record detail")
    public Uni<BillingRecordResponse> getRecord(@PathParam("id") UUID id) {
        return getBillingRecordUseCase.execute(id).map(BillingMapper::toResponse);
    }
}
