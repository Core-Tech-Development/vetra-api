package dev.vetra.api.modules.billing.resource;

import dev.vetra.api.modules.billing.dto.BillingDashboardResponse;
import dev.vetra.api.modules.billing.dto.BillingMapper;
import dev.vetra.api.modules.billing.dto.BillingRecordResponse;
import dev.vetra.api.modules.billing.usecase.GetBillingDashboardUseCase;
import dev.vetra.api.modules.billing.usecase.GetBillingRecordUseCase;
import dev.vetra.api.modules.billing.usecase.ListBillingRecordsUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
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

@Path("/api/v1/admin/billing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"PLATFORM_ADMIN", "PLATFORM_OPERATOR"})
@Tag(name = "Billing Admin", description = "Platform billing management")
public class BillingAdminResource {

    private final GetBillingDashboardUseCase getDashboardUseCase;
    private final ListBillingRecordsUseCase listBillingRecordsUseCase;
    private final GetBillingRecordUseCase getBillingRecordUseCase;

    @Inject
    public BillingAdminResource(GetBillingDashboardUseCase getDashboardUseCase,
                                 ListBillingRecordsUseCase listBillingRecordsUseCase,
                                 GetBillingRecordUseCase getBillingRecordUseCase) {
        this.getDashboardUseCase = getDashboardUseCase;
        this.listBillingRecordsUseCase = listBillingRecordsUseCase;
        this.getBillingRecordUseCase = getBillingRecordUseCase;
    }

    @GET
    @Path("/dashboard")
    @Operation(summary = "Get billing dashboard statistics")
    public Uni<BillingDashboardResponse> getDashboard() {
        return getDashboardUseCase.execute()
                .map(r -> new BillingDashboardResponse(
                        r.totalRevenueCents(), r.totalPlatformFeeCents(),
                        r.pendingPayments(), r.overduePayments(), r.totalBillingRecords()));
    }

    @GET
    @Path("/records")
    @Operation(summary = "List all billing records")
    public Uni<PageResponse<BillingRecordResponse>> listRecords(
            @QueryParam("page") @DefaultValue("0") Integer page,
            @QueryParam("size") @DefaultValue("20") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);

        Uni<List<BillingRecordResponse>> items = listBillingRecordsUseCase.execute(pageRequest.page(), pageRequest.size())
                .map(list -> list.stream().map(BillingMapper::toResponse).toList());
        Uni<Long> total = listBillingRecordsUseCase.count();

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
