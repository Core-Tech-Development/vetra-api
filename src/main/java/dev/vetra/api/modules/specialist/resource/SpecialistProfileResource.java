package dev.vetra.api.modules.specialist.resource;

import dev.vetra.api.modules.billing.dto.BillingMapper;
import dev.vetra.api.modules.billing.dto.CreateSpecialistPricingRequest;
import dev.vetra.api.modules.billing.dto.SpecialistPricingResponse;
import dev.vetra.api.modules.billing.repository.SpecialistPricingRepository;
import dev.vetra.api.modules.billing.usecase.UpsertSpecialistPricingUseCase;
import dev.vetra.api.modules.specialist.dto.AddCoverageAreaRequest;
import dev.vetra.api.modules.specialist.dto.CoverageAreaResponse;
import dev.vetra.api.modules.specialist.dto.SpecialistMapper;
import dev.vetra.api.modules.specialist.dto.SpecialistResponse;
import dev.vetra.api.modules.specialist.dto.UpdateMyProfileRequest;
import dev.vetra.api.modules.specialist.usecase.AddCoverageAreaUseCase;
import dev.vetra.api.modules.specialist.usecase.GetMyProfileUseCase;
import dev.vetra.api.modules.specialist.usecase.ListCoverageAreasUseCase;
import dev.vetra.api.modules.specialist.usecase.RemoveCoverageAreaUseCase;
import dev.vetra.api.modules.specialist.usecase.ToggleCoverageAreaUseCase;
import dev.vetra.api.modules.specialist.usecase.UpdateMyProfileUseCase;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/specialist/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("SPECIALIST")
@Tag(name = "Specialist Profile", description = "Specialist self-service profile management endpoints")
public class SpecialistProfileResource {

    private static final Logger LOG = Logger.getLogger(SpecialistProfileResource.class);

    private final SecurityContext securityContext;
    private final GetMyProfileUseCase getMyProfileUseCase;
    private final UpdateMyProfileUseCase updateMyProfileUseCase;
    private final AddCoverageAreaUseCase addCoverageAreaUseCase;
    private final ListCoverageAreasUseCase listCoverageAreasUseCase;
    private final RemoveCoverageAreaUseCase removeCoverageAreaUseCase;
    private final ToggleCoverageAreaUseCase toggleCoverageAreaUseCase;
    private final SpecialistPricingRepository specialistPricingRepository;
    private final UpsertSpecialistPricingUseCase upsertSpecialistPricingUseCase;

    @Inject
    public SpecialistProfileResource(SecurityContext securityContext,
                                     GetMyProfileUseCase getMyProfileUseCase,
                                     UpdateMyProfileUseCase updateMyProfileUseCase,
                                     AddCoverageAreaUseCase addCoverageAreaUseCase,
                                     ListCoverageAreasUseCase listCoverageAreasUseCase,
                                     RemoveCoverageAreaUseCase removeCoverageAreaUseCase,
                                     ToggleCoverageAreaUseCase toggleCoverageAreaUseCase,
                                     SpecialistPricingRepository specialistPricingRepository,
                                     UpsertSpecialistPricingUseCase upsertSpecialistPricingUseCase) {
        this.securityContext = securityContext;
        this.getMyProfileUseCase = getMyProfileUseCase;
        this.updateMyProfileUseCase = updateMyProfileUseCase;
        this.addCoverageAreaUseCase = addCoverageAreaUseCase;
        this.listCoverageAreasUseCase = listCoverageAreasUseCase;
        this.removeCoverageAreaUseCase = removeCoverageAreaUseCase;
        this.toggleCoverageAreaUseCase = toggleCoverageAreaUseCase;
        this.specialistPricingRepository = specialistPricingRepository;
        this.upsertSpecialistPricingUseCase = upsertSpecialistPricingUseCase;
    }

    @GET
    @Operation(summary = "Get own profile", description = "Retrieves the specialist profile for the currently authenticated user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile found",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> getProfile() {
        String userId = extractUserId();
        LOG.debugf("GET profile for userId=%s", userId);
        return getMyProfileUseCase.execute(userId)
                .map(specialist -> Response.ok(SpecialistMapper.toResponse(specialist)).build());
    }

    @PUT
    @Operation(summary = "Update own profile", description = "Updates the specialist's own profile information. Only non-sensitive fields can be changed.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> updateProfile(@Valid UpdateMyProfileRequest request) {
        String userId = extractUserId();
        LOG.debugf("PUT profile for userId=%s", userId);
        return updateMyProfileUseCase.execute(userId, request)
                .map(specialist -> Response.ok(SpecialistMapper.toResponse(specialist)).build());
    }

    @GET
    @Path("/coverage-areas")
    @Operation(summary = "List own coverage areas", description = "Lists all coverage areas for the currently authenticated specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Coverage areas list"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> listCoverageAreas() {
        String userId = extractUserId();
        LOG.debugf("GET coverage-areas for userId=%s", userId);
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> listCoverageAreasUseCase.execute(specialist.id()))
                .map(areas -> {
                    List<CoverageAreaResponse> response = areas.stream()
                            .map(SpecialistMapper::toResponse)
                            .toList();
                    return Response.ok(response).build();
                });
    }

    @POST
    @Path("/coverage-areas")
    @Operation(summary = "Add coverage area", description = "Adds a new coverage area to the currently authenticated specialist")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Coverage area added",
                    content = @Content(schema = @Schema(implementation = CoverageAreaResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> addCoverageArea(@Valid AddCoverageAreaRequest request) {
        String userId = extractUserId();
        LOG.debugf("POST coverage-area for userId=%s", userId);
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> addCoverageAreaUseCase.execute(specialist.id(), request))
                .map(area -> {
                    CoverageAreaResponse body = SpecialistMapper.toResponse(area);
                    return Response.created(URI.create("/api/v1/specialist/profile/coverage-areas/" + area.id()))
                            .entity(body)
                            .build();
                });
    }

    @PATCH
    @Path("/coverage-areas/{areaId}/toggle")
    @Operation(summary = "Toggle coverage area active status", description = "Toggles the active/inactive status of a coverage area owned by the authenticated specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Coverage area toggled",
                    content = @Content(schema = @Schema(implementation = CoverageAreaResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Coverage area or specialist profile not found"),
            @APIResponse(responseCode = "422", description = "Coverage area does not belong to this specialist")
    })
    public Uni<Response> toggleCoverageArea(
            @PathParam("areaId")
            @Parameter(description = "Coverage area UUID") UUID areaId) {
        String userId = extractUserId();
        LOG.debugf("PATCH toggle coverage-area areaId=%s for userId=%s", areaId, userId);
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> toggleCoverageAreaUseCase.execute(areaId, specialist.id()))
                .map(area -> Response.ok(SpecialistMapper.toResponse(area)).build());
    }

    @DELETE
    @Path("/coverage-areas/{areaId}")
    @Operation(summary = "Remove coverage area", description = "Removes a coverage area by its ID for the authenticated specialist")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Coverage area removed"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Coverage area not found")
    })
    public Uni<Response> removeCoverageArea(
            @PathParam("areaId")
            @Parameter(description = "Coverage area UUID") UUID areaId) {
        String userId = extractUserId();
        LOG.debugf("DELETE coverage-area areaId=%s for userId=%s", areaId, userId);
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> {
                    // Verify ownership before removing
                    return removeCoverageAreaUseCase.execute(areaId);
                })
                .map(ignored -> Response.noContent().build());
    }

    // ---- Pricing endpoints ----

    @GET
    @Path("/pricing")
    @Operation(summary = "List own exam pricing", description = "Lists all exam pricing set by the currently authenticated specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Pricing list"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> listPricing() {
        String userId = extractUserId();
        LOG.debugf("GET pricing for userId=%s", userId);
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> specialistPricingRepository.findBySpecialistId(specialist.id()))
                .map(pricings -> {
                    List<SpecialistPricingResponse> response = pricings.stream()
                            .map(BillingMapper::toResponse)
                            .toList();
                    return Response.ok(response).build();
                });
    }

    @PUT
    @Path("/pricing")
    @Operation(summary = "Upsert exam pricing", description = "Creates or updates the specialist's pricing for a specific exam type")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Pricing upserted",
                    content = @Content(schema = @Schema(implementation = SpecialistPricingResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — SPECIALIST role required"),
            @APIResponse(responseCode = "404", description = "Specialist profile not found for current user")
    })
    public Uni<Response> upsertPricing(@Valid CreateSpecialistPricingRequest request) {
        String userId = extractUserId();
        LOG.debugf("PUT pricing for userId=%s, examType=%s", userId, request.examType());
        return getMyProfileUseCase.execute(userId)
                .flatMap(specialist -> upsertSpecialistPricingUseCase.execute(
                        specialist.id(), request.examType(), request.priceCents()))
                .map(pricing -> Response.ok(BillingMapper.toResponse(pricing)).build());
    }

    private String extractUserId() {
        return securityContext.userId()
                .orElseThrow(() -> new SecurityException("Unable to extract user ID from security context"));
    }
}
