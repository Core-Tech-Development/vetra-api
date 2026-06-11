package dev.vetra.api.modules.clinic.resource;

import dev.vetra.api.modules.clinic.dto.ClinicMapper;
import dev.vetra.api.modules.clinic.dto.ClinicResponse;
import dev.vetra.api.modules.clinic.dto.CreateClinicRequest;
import dev.vetra.api.modules.clinic.repository.ClinicRepository;
import dev.vetra.api.modules.clinic.usecase.CreateClinicUseCase;
import dev.vetra.api.modules.clinic.usecase.GetClinicUseCase;
import dev.vetra.api.modules.clinic.usecase.ListClinicsUseCase;
import dev.vetra.api.shared.exception.NotFoundException;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
@Tag(name = "Clinics", description = "Clinic management endpoints")
public class ClinicResource {

    private final CreateClinicUseCase createClinicUseCase;
    private final GetClinicUseCase getClinicUseCase;
    private final ListClinicsUseCase listClinicsUseCase;
    private final ClinicRepository clinicRepository;

    @Inject
    public ClinicResource(CreateClinicUseCase createClinicUseCase,
                          GetClinicUseCase getClinicUseCase,
                          ListClinicsUseCase listClinicsUseCase,
                          ClinicRepository clinicRepository) {
        this.createClinicUseCase = createClinicUseCase;
        this.getClinicUseCase = getClinicUseCase;
        this.listClinicsUseCase = listClinicsUseCase;
        this.clinicRepository = clinicRepository;
    }

    @POST
    @Path("/clinics")
    @Operation(summary = "Create a new clinic", description = "Registers a new veterinary clinic with PENDING_APPROVAL status")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Clinic created",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "409", description = "Clinic with this document already exists")
    })
    public Uni<Response> create(@Valid CreateClinicRequest request) {
        return createClinicUseCase.execute(request)
                .map(clinic -> {
                    ClinicResponse body = ClinicMapper.toResponse(clinic);
                    return Response.created(URI.create("/api/v1/clinics/" + clinic.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/clinics/by-email")
    @Operation(summary = "Find clinic by email", description = "Returns the clinic associated with the given email address")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic found",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "404", description = "Clinic not found")
    })
    public Uni<Response> findByEmail(
            @QueryParam("email") @Parameter(description = "Email address") String email) {
        return clinicRepository.findByEmail(email)
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Clinic", "email: " + email)))
                .map(clinic -> {
                    ClinicResponse body = ClinicMapper.toResponse(clinic);
                    return Response.ok(body).build();
                });
    }

    @GET
    @Path("/clinics/{id}")
    @Operation(summary = "Get clinic by ID", description = "Retrieves a single clinic by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic found",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "404", description = "Clinic not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Clinic UUID") UUID id) {
        return getClinicUseCase.execute(id)
                .map(clinic -> Response.ok(ClinicMapper.toResponse(clinic)).build());
    }

    @GET
    @Path("/clinics")
    @Operation(summary = "List clinics", description = "Returns a paginated list of clinics")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated clinic list")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listClinicsUseCase.execute(pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }
}
