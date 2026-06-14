package dev.vetra.api.modules.billing.resource;

import dev.vetra.api.modules.billing.dto.BillingMapper;
import dev.vetra.api.modules.billing.dto.CreateExamTypePricingRequest;
import dev.vetra.api.modules.billing.dto.CreateSpecialistPricingRequest;
import dev.vetra.api.modules.billing.dto.ExamTypePricingResponse;
import dev.vetra.api.modules.billing.dto.UpdateExamTypePricingRequest;
import dev.vetra.api.modules.billing.usecase.CreateExamTypePricingUseCase;
import dev.vetra.api.modules.billing.usecase.CreateSpecialistPricingUseCase;
import dev.vetra.api.modules.billing.usecase.ListExamTypePricingsUseCase;
import dev.vetra.api.modules.billing.usecase.ListSpecialistPricingsUseCase;
import dev.vetra.api.modules.billing.usecase.UpdateExamTypePricingUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/admin/pricing")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATFORM_ADMIN")
@Tag(name = "Pricing", description = "Exam type and specialist pricing management")
public class PricingResource {

    private final CreateExamTypePricingUseCase createExamTypePricingUseCase;
    private final UpdateExamTypePricingUseCase updateExamTypePricingUseCase;
    private final ListExamTypePricingsUseCase listExamTypePricingsUseCase;
    private final CreateSpecialistPricingUseCase createSpecialistPricingUseCase;
    private final ListSpecialistPricingsUseCase listSpecialistPricingsUseCase;

    @Inject
    public PricingResource(CreateExamTypePricingUseCase createExamTypePricingUseCase,
                            UpdateExamTypePricingUseCase updateExamTypePricingUseCase,
                            ListExamTypePricingsUseCase listExamTypePricingsUseCase,
                            CreateSpecialistPricingUseCase createSpecialistPricingUseCase,
                            ListSpecialistPricingsUseCase listSpecialistPricingsUseCase) {
        this.createExamTypePricingUseCase = createExamTypePricingUseCase;
        this.updateExamTypePricingUseCase = updateExamTypePricingUseCase;
        this.listExamTypePricingsUseCase = listExamTypePricingsUseCase;
        this.createSpecialistPricingUseCase = createSpecialistPricingUseCase;
        this.listSpecialistPricingsUseCase = listSpecialistPricingsUseCase;
    }

    @POST
    @Path("/exam-types")
    @Operation(summary = "Create exam type pricing")
    public Uni<Response> createExamTypePricing(@Valid CreateExamTypePricingRequest request) {
        return createExamTypePricingUseCase.execute(request.examType(), request.priceCents(), request.platformFeePercent())
                .map(BillingMapper::toResponse)
                .map(r -> Response.status(201).entity(r).build());
    }

    @GET
    @Path("/exam-types")
    @Operation(summary = "List all exam type pricings")
    public Uni<List<ExamTypePricingResponse>> listExamTypePricings() {
        return listExamTypePricingsUseCase.execute()
                .map(list -> list.stream().map(BillingMapper::toResponse).toList());
    }

    @PUT
    @Path("/exam-types/{id}")
    @Operation(summary = "Update exam type pricing")
    public Uni<ExamTypePricingResponse> updateExamTypePricing(@PathParam("id") UUID id,
                                                               @Valid UpdateExamTypePricingRequest request) {
        return updateExamTypePricingUseCase.execute(id, request.priceCents(), request.platformFeePercent(), request.active())
                .map(BillingMapper::toResponse);
    }

    @POST
    @Path("/specialists/{specialistId}")
    @Operation(summary = "Create specialist pricing override")
    public Uni<Response> createSpecialistPricing(@PathParam("specialistId") UUID specialistId,
                                                  @Valid CreateSpecialistPricingRequest request) {
        return createSpecialistPricingUseCase.execute(specialistId, request.examType(), request.priceCents())
                .map(sp -> Response.status(201).entity(sp).build());
    }

    @GET
    @Path("/specialists/{specialistId}")
    @Operation(summary = "List specialist pricing overrides")
    public Uni<Response> listSpecialistPricings(@PathParam("specialistId") UUID specialistId) {
        return listSpecialistPricingsUseCase.execute(specialistId)
                .map(list -> Response.ok(list).build());
    }
}
