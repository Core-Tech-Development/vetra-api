package dev.vetra.api.modules.specialist.resource;

import dev.vetra.api.modules.specialist.dto.AddCoverageAreaRequest;
import dev.vetra.api.modules.specialist.dto.CoverageAreaResponse;
import dev.vetra.api.modules.specialist.dto.CreateSpecialistRequest;
import dev.vetra.api.modules.specialist.dto.SpecialistMapper;
import dev.vetra.api.modules.specialist.dto.SpecialistResponse;
import dev.vetra.api.modules.specialist.dto.UpdateSpecialistRequest;
import dev.vetra.api.modules.specialist.usecase.AddCoverageAreaUseCase;
import dev.vetra.api.modules.specialist.usecase.ApproveSpecialistUseCase;
import dev.vetra.api.modules.specialist.usecase.CreateSpecialistUseCase;
import dev.vetra.api.modules.specialist.usecase.GetSpecialistUseCase;
import dev.vetra.api.modules.specialist.usecase.ListCoverageAreasUseCase;
import dev.vetra.api.modules.specialist.usecase.ListSpecialistsUseCase;
import dev.vetra.api.modules.specialist.usecase.RemoveCoverageAreaUseCase;
import dev.vetra.api.modules.specialist.usecase.SearchAvailableSpecialistsUseCase;
import dev.vetra.api.modules.specialist.usecase.UpdateSpecialistUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Specialists", description = "Specialist management endpoints")
public class SpecialistResource {

    private final CreateSpecialistUseCase createSpecialistUseCase;
    private final GetSpecialistUseCase getSpecialistUseCase;
    private final ListSpecialistsUseCase listSpecialistsUseCase;
    private final UpdateSpecialistUseCase updateSpecialistUseCase;
    private final ApproveSpecialistUseCase approveSpecialistUseCase;
    private final AddCoverageAreaUseCase addCoverageAreaUseCase;
    private final ListCoverageAreasUseCase listCoverageAreasUseCase;
    private final RemoveCoverageAreaUseCase removeCoverageAreaUseCase;
    private final SearchAvailableSpecialistsUseCase searchAvailableSpecialistsUseCase;

    @Inject
    public SpecialistResource(CreateSpecialistUseCase createSpecialistUseCase,
                              GetSpecialistUseCase getSpecialistUseCase,
                              ListSpecialistsUseCase listSpecialistsUseCase,
                              UpdateSpecialistUseCase updateSpecialistUseCase,
                              ApproveSpecialistUseCase approveSpecialistUseCase,
                              AddCoverageAreaUseCase addCoverageAreaUseCase,
                              ListCoverageAreasUseCase listCoverageAreasUseCase,
                              RemoveCoverageAreaUseCase removeCoverageAreaUseCase,
                              SearchAvailableSpecialistsUseCase searchAvailableSpecialistsUseCase) {
        this.createSpecialistUseCase = createSpecialistUseCase;
        this.getSpecialistUseCase = getSpecialistUseCase;
        this.listSpecialistsUseCase = listSpecialistsUseCase;
        this.updateSpecialistUseCase = updateSpecialistUseCase;
        this.approveSpecialistUseCase = approveSpecialistUseCase;
        this.addCoverageAreaUseCase = addCoverageAreaUseCase;
        this.listCoverageAreasUseCase = listCoverageAreasUseCase;
        this.removeCoverageAreaUseCase = removeCoverageAreaUseCase;
        this.searchAvailableSpecialistsUseCase = searchAvailableSpecialistsUseCase;
    }

    @POST
    @Path("/specialists")
    @Operation(summary = "Create a new specialist", description = "Registers a new veterinary imaging specialist with PENDING_APPROVAL status")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Specialist created",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "409", description = "Specialist with this CRMV already exists")
    })
    public Uni<Response> create(@Valid CreateSpecialistRequest request) {
        // TODO: replace with identity.getPrincipal().getName() once Keycloak auth is fully wired
        // Each specialist needs a unique user_id (Keycloak subject). Using UUID placeholder until real OIDC flow.
        String userId = UUID.randomUUID().toString();
        return createSpecialistUseCase.execute(request, userId)
                .map(specialist -> {
                    SpecialistResponse body = SpecialistMapper.toResponse(specialist);
                    return Response.created(URI.create("/api/v1/specialists/" + specialist.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/specialists")
    @Operation(summary = "List specialists", description = "Returns a paginated list of specialists")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated specialist list")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listSpecialistsUseCase.execute(pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/specialists/{id}")
    @Operation(summary = "Get specialist by ID", description = "Retrieves a single specialist by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Specialist found",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "404", description = "Specialist not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id) {
        return getSpecialistUseCase.execute(id)
                .map(specialist -> Response.ok(SpecialistMapper.toResponse(specialist)).build());
    }

    @PUT
    @Path("/specialists/{id}")
    @Operation(summary = "Update specialist", description = "Updates an existing specialist's information")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Specialist updated",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Specialist not found")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id,
            @Valid UpdateSpecialistRequest request) {
        return updateSpecialistUseCase.execute(id, request)
                .map(specialist -> Response.ok(SpecialistMapper.toResponse(specialist)).build());
    }

    @PATCH
    @Path("/specialists/{id}/approve")
    @Operation(summary = "Approve specialist", description = "Changes specialist status from PENDING_APPROVAL to ACTIVE. Admin only.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Specialist approved",
                    content = @Content(schema = @Schema(implementation = SpecialistResponse.class))),
            @APIResponse(responseCode = "404", description = "Specialist not found"),
            @APIResponse(responseCode = "422", description = "Specialist is not in PENDING_APPROVAL status"),
            @APIResponse(responseCode = "403", description = "Forbidden — admin role required")
    })
    public Uni<Response> approve(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id) {
        return approveSpecialistUseCase.execute(id)
                .map(specialist -> Response.ok(SpecialistMapper.toResponse(specialist)).build());
    }

    @GET
    @Path("/specialists/{id}/coverage-areas")
    @Operation(summary = "List coverage areas", description = "Lists all coverage areas for a specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Coverage areas list")
    })
    public Uni<Response> listCoverageAreas(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id) {
        return listCoverageAreasUseCase.execute(id)
                .map(areas -> {
                    List<CoverageAreaResponse> response = areas.stream()
                            .map(SpecialistMapper::toResponse)
                            .toList();
                    return Response.ok(response).build();
                });
    }

    @POST
    @Path("/specialists/{id}/coverage-areas")
    @Operation(summary = "Add coverage area", description = "Adds a new coverage area to a specialist")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Coverage area added",
                    content = @Content(schema = @Schema(implementation = CoverageAreaResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Specialist not found")
    })
    public Uni<Response> addCoverageArea(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id,
            @Valid AddCoverageAreaRequest request) {
        return addCoverageAreaUseCase.execute(id, request)
                .map(area -> {
                    CoverageAreaResponse body = SpecialistMapper.toResponse(area);
                    return Response.created(URI.create("/api/v1/specialists/" + id + "/coverage-areas/" + area.id()))
                            .entity(body)
                            .build();
                });
    }

    @DELETE
    @Path("/specialists/{id}/coverage-areas/{areaId}")
    @Operation(summary = "Remove coverage area", description = "Removes a coverage area by its ID")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Coverage area removed"),
            @APIResponse(responseCode = "404", description = "Coverage area not found")
    })
    public Uni<Response> removeCoverageArea(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id,
            @PathParam("areaId")
            @Parameter(description = "Coverage area UUID") UUID areaId) {
        return removeCoverageAreaUseCase.execute(areaId)
                .map(ignored -> Response.noContent().build());
    }

    @GET
    @Path("/specialists/search")
    @Operation(summary = "Search available specialists", description = "Searches for ACTIVE specialists by city and state, optionally filtered by specialty")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of matching specialists")
    })
    public Uni<Response> search(
            @QueryParam("city")
            @Parameter(description = "City to search in", required = true) String city,
            @QueryParam("state")
            @Parameter(description = "State code (2 letters)", required = true) String state,
            @QueryParam("specialty")
            @Parameter(description = "Specialty name (optional filter)") String specialty) {
        return searchAvailableSpecialistsUseCase.execute(city, state, specialty)
                .map(specialists -> Response.ok(specialists).build());
    }
}
