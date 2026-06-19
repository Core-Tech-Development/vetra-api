package dev.vetra.api.modules.tutor.resource;

import dev.vetra.api.modules.tutor.dto.CreateTutorRequest;
import dev.vetra.api.modules.tutor.dto.TutorMapper;
import dev.vetra.api.modules.tutor.dto.TutorResponse;
import dev.vetra.api.modules.tutor.dto.UpdateTutorRequest;
import dev.vetra.api.modules.tutor.usecase.CreateTutorUseCase;
import dev.vetra.api.modules.tutor.usecase.DeleteTutorUseCase;
import dev.vetra.api.modules.tutor.usecase.GetTutorUseCase;
import dev.vetra.api.modules.tutor.usecase.ListTutorsByClinicUseCase;
import dev.vetra.api.modules.tutor.usecase.UpdateTutorUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
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
@Tag(name = "Tutors", description = "Tutor (pet owner) management endpoints")
public class TutorResource {

    private final CreateTutorUseCase createTutorUseCase;
    private final DeleteTutorUseCase deleteTutorUseCase;
    private final GetTutorUseCase getTutorUseCase;
    private final ListTutorsByClinicUseCase listTutorsByClinicUseCase;
    private final UpdateTutorUseCase updateTutorUseCase;

    @Inject
    public TutorResource(CreateTutorUseCase createTutorUseCase,
                         DeleteTutorUseCase deleteTutorUseCase,
                         GetTutorUseCase getTutorUseCase,
                         ListTutorsByClinicUseCase listTutorsByClinicUseCase,
                         UpdateTutorUseCase updateTutorUseCase) {
        this.createTutorUseCase = createTutorUseCase;
        this.deleteTutorUseCase = deleteTutorUseCase;
        this.getTutorUseCase = getTutorUseCase;
        this.listTutorsByClinicUseCase = listTutorsByClinicUseCase;
        this.updateTutorUseCase = updateTutorUseCase;
    }

    @POST
    @Path("/clinics/{clinicId}/tutors")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Create a new tutor", description = "Registers a new tutor (pet owner) for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Tutor created",
                    content = @Content(schema = @Schema(implementation = TutorResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Clinic not found"),
            @APIResponse(responseCode = "409", description = "Tutor with this document already exists in the clinic")
    })
    public Uni<Response> create(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @Valid CreateTutorRequest request) {
        return createTutorUseCase.execute(clinicId, request)
                .map(tutor -> {
                    TutorResponse body = TutorMapper.toResponse(tutor);
                    return Response.created(URI.create("/api/v1/tutors/" + tutor.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/clinics/{clinicId}/tutors")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "List tutors by clinic", description = "Returns a paginated list of tutors for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated tutor list")
    })
    public Uni<Response> listByClinic(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listTutorsByClinicUseCase.execute(clinicId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/tutors/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF", "SPECIALIST", "PLATFORM_ADMIN"})
    @Operation(summary = "Get tutor by ID", description = "Retrieves a single tutor by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tutor found",
                    content = @Content(schema = @Schema(implementation = TutorResponse.class))),
            @APIResponse(responseCode = "404", description = "Tutor not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Tutor UUID") UUID id) {
        return getTutorUseCase.execute(id)
                .map(tutor -> Response.ok(TutorMapper.toResponse(tutor)).build());
    }

    @PUT
    @Path("/tutors/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Update tutor", description = "Updates an existing tutor's information")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Tutor updated",
                    content = @Content(schema = @Schema(implementation = TutorResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Tutor not found"),
            @APIResponse(responseCode = "409", description = "Tutor with this document already exists in the clinic")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Tutor UUID") UUID id,
            @Valid UpdateTutorRequest request) {
        return updateTutorUseCase.execute(id, request)
                .map(tutor -> Response.ok(TutorMapper.toResponse(tutor)).build());
    }

    @DELETE
    @Path("/tutors/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Delete tutor", description = "Deletes a tutor. The tutor must have no linked patients.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Tutor deleted"),
            @APIResponse(responseCode = "404", description = "Tutor not found"),
            @APIResponse(responseCode = "422", description = "Tutor has linked patients")
    })
    public Uni<Response> delete(@PathParam("id") @Parameter(description = "Tutor UUID") UUID id) {
        return deleteTutorUseCase.execute(id)
                .map(ignored -> Response.noContent().build());
    }
}
