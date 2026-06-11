package dev.vetra.api.modules.clinic.resource;

import dev.vetra.api.modules.clinic.dto.ClinicStaffMapper;
import dev.vetra.api.modules.clinic.dto.ClinicStaffResponse;
import dev.vetra.api.modules.clinic.dto.CreateClinicStaffRequest;
import dev.vetra.api.modules.clinic.dto.UpdateClinicStaffRequest;
import dev.vetra.api.modules.clinic.usecase.CreateClinicStaffUseCase;
import dev.vetra.api.modules.clinic.usecase.DeactivateClinicStaffUseCase;
import dev.vetra.api.modules.clinic.usecase.GetClinicStaffUseCase;
import dev.vetra.api.modules.clinic.usecase.ListClinicStaffUseCase;
import dev.vetra.api.modules.clinic.usecase.UpdateClinicStaffUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clinic Staff", description = "Clinic staff (veterinarians and secretaries) management endpoints")
public class ClinicStaffResource {

    private final CreateClinicStaffUseCase createClinicStaffUseCase;
    private final ListClinicStaffUseCase listClinicStaffUseCase;
    private final GetClinicStaffUseCase getClinicStaffUseCase;
    private final UpdateClinicStaffUseCase updateClinicStaffUseCase;
    private final DeactivateClinicStaffUseCase deactivateClinicStaffUseCase;

    @Inject
    public ClinicStaffResource(CreateClinicStaffUseCase createClinicStaffUseCase,
                               ListClinicStaffUseCase listClinicStaffUseCase,
                               GetClinicStaffUseCase getClinicStaffUseCase,
                               UpdateClinicStaffUseCase updateClinicStaffUseCase,
                               DeactivateClinicStaffUseCase deactivateClinicStaffUseCase) {
        this.createClinicStaffUseCase = createClinicStaffUseCase;
        this.listClinicStaffUseCase = listClinicStaffUseCase;
        this.getClinicStaffUseCase = getClinicStaffUseCase;
        this.updateClinicStaffUseCase = updateClinicStaffUseCase;
        this.deactivateClinicStaffUseCase = deactivateClinicStaffUseCase;
    }

    @POST
    @Path("/clinics/{clinicId}/staff")
    @Operation(summary = "Create a new clinic staff member", description = "Registers a new staff member (veterinarian or secretary) for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Staff member created",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Clinic not found"),
            @APIResponse(responseCode = "409", description = "Staff member with this email already exists"),
            @APIResponse(responseCode = "422", description = "Clinic is not active")
    })
    public Uni<Response> create(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @Valid CreateClinicStaffRequest request) {
        return createClinicStaffUseCase.execute(clinicId, request)
                .map(staff -> {
                    ClinicStaffResponse body = ClinicStaffMapper.toResponse(staff);
                    return Response.created(URI.create("/api/v1/staff/" + staff.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/clinics/{clinicId}/staff")
    @Operation(summary = "List staff by clinic", description = "Returns a paginated list of staff members for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated staff list")
    })
    public Uni<Response> listByClinic(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listClinicStaffUseCase.execute(clinicId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/staff/{id}")
    @Operation(summary = "Get staff member by ID", description = "Retrieves a single staff member by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Staff member found",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "404", description = "Staff member not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Staff UUID") UUID id) {
        return getClinicStaffUseCase.execute(id)
                .map(staff -> Response.ok(ClinicStaffMapper.toResponse(staff)).build());
    }

    @PUT
    @Path("/staff/{id}")
    @Operation(summary = "Update staff member", description = "Updates an existing staff member's information")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Staff member updated",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Staff member not found")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Staff UUID") UUID id,
            @Valid UpdateClinicStaffRequest request) {
        return updateClinicStaffUseCase.execute(id, request)
                .map(staff -> Response.ok(ClinicStaffMapper.toResponse(staff)).build());
    }

    @DELETE
    @Path("/staff/{id}")
    @Operation(summary = "Deactivate staff member", description = "Deactivates a staff member (soft delete). Disables the corresponding Keycloak user.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Staff member deactivated",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "404", description = "Staff member not found"),
            @APIResponse(responseCode = "422", description = "Staff member is already inactive")
    })
    public Uni<Response> deactivate(
            @PathParam("id")
            @Parameter(description = "Staff UUID") UUID id) {
        return deactivateClinicStaffUseCase.execute(id)
                .map(staff -> Response.ok(ClinicStaffMapper.toResponse(staff)).build());
    }
}
