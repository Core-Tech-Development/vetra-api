package dev.vetra.api.modules.laudo.resource;

import dev.vetra.api.modules.laudo.dto.CreateLaudoRequest;
import dev.vetra.api.modules.laudo.dto.LaudoMapper;
import dev.vetra.api.modules.laudo.dto.LaudoResponse;
import dev.vetra.api.modules.laudo.dto.UpdateLaudoRequest;
import dev.vetra.api.modules.laudo.usecase.CreateLaudoUseCase;
import dev.vetra.api.modules.laudo.usecase.GetLaudoByAppointmentUseCase;
import dev.vetra.api.modules.laudo.usecase.GetLaudoUseCase;
import dev.vetra.api.modules.laudo.usecase.IssueLaudoUseCase;
import dev.vetra.api.modules.laudo.usecase.ListLaudosBySpecialistUseCase;
import dev.vetra.api.modules.laudo.usecase.UpdateLaudoUseCase;
import dev.vetra.api.modules.scheduling.repository.AppointmentRepository;
import dev.vetra.api.shared.exception.NotFoundException;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Laudos", description = "Diagnostic laudo management endpoints")
public class LaudoResource {

    private final CreateLaudoUseCase createLaudoUseCase;
    private final GetLaudoUseCase getLaudoUseCase;
    private final UpdateLaudoUseCase updateLaudoUseCase;
    private final IssueLaudoUseCase issueLaudoUseCase;
    private final ListLaudosBySpecialistUseCase listLaudosBySpecialistUseCase;
    private final GetLaudoByAppointmentUseCase getLaudoByAppointmentUseCase;
    private final AppointmentRepository appointmentRepository;

    @Inject
    public LaudoResource(CreateLaudoUseCase createLaudoUseCase,
                          GetLaudoUseCase getLaudoUseCase,
                          UpdateLaudoUseCase updateLaudoUseCase,
                          IssueLaudoUseCase issueLaudoUseCase,
                          ListLaudosBySpecialistUseCase listLaudosBySpecialistUseCase,
                          GetLaudoByAppointmentUseCase getLaudoByAppointmentUseCase,
                          AppointmentRepository appointmentRepository) {
        this.createLaudoUseCase = createLaudoUseCase;
        this.getLaudoUseCase = getLaudoUseCase;
        this.updateLaudoUseCase = updateLaudoUseCase;
        this.issueLaudoUseCase = issueLaudoUseCase;
        this.listLaudosBySpecialistUseCase = listLaudosBySpecialistUseCase;
        this.getLaudoByAppointmentUseCase = getLaudoByAppointmentUseCase;
        this.appointmentRepository = appointmentRepository;
    }

    @POST
    @Path("/appointments/{appointmentId}/laudos")
    @Operation(summary = "Create draft laudo", description = "Creates a new draft diagnostic laudo for an appointment")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Laudo created",
                    content = @Content(schema = @Schema(implementation = LaudoResponse.class))),
            @APIResponse(responseCode = "409", description = "Laudo already exists for this appointment")
    })
    public Uni<Response> create(
            @PathParam("appointmentId")
            @Parameter(description = "Appointment UUID") UUID appointmentId,
            @Valid CreateLaudoRequest request) {
        // Derive specialistId from the appointment
        return appointmentRepository.findById(appointmentId)
                .flatMap(optAppointment -> {
                    if (optAppointment.isEmpty()) {
                        return Uni.createFrom().failure(
                                new NotFoundException("Appointment", appointmentId.toString()));
                    }
                    UUID specialistId = optAppointment.get().specialistId();
                    return createLaudoUseCase.execute(appointmentId, specialistId, request)
                            .map(laudo -> {
                                LaudoResponse body = LaudoMapper.toResponse(laudo);
                                return Response.created(URI.create("/api/v1/laudos/" + laudo.id()))
                                        .entity(body)
                                        .build();
                            });
                });
    }

    @GET
    @Path("/laudos/{id}")
    @Operation(summary = "Get laudo by ID", description = "Retrieves a single laudo by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Laudo found",
                    content = @Content(schema = @Schema(implementation = LaudoResponse.class))),
            @APIResponse(responseCode = "404", description = "Laudo not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Laudo UUID") UUID id) {
        return getLaudoUseCase.execute(id)
                .map(laudo -> Response.ok(LaudoMapper.toResponse(laudo)).build());
    }

    @PUT
    @Path("/laudos/{id}")
    @Operation(summary = "Update draft laudo", description = "Updates findings, conclusion, and recommendations of a draft laudo")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Laudo updated",
                    content = @Content(schema = @Schema(implementation = LaudoResponse.class))),
            @APIResponse(responseCode = "404", description = "Laudo not found"),
            @APIResponse(responseCode = "422", description = "Laudo is not in DRAFT status")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Laudo UUID") UUID id,
            @Valid UpdateLaudoRequest request) {
        return updateLaudoUseCase.execute(id, request)
                .map(laudo -> Response.ok(LaudoMapper.toResponse(laudo)).build());
    }

    @PATCH
    @Path("/laudos/{id}/issue")
    @Operation(summary = "Issue laudo", description = "Transitions a draft laudo to ISSUED status")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Laudo issued",
                    content = @Content(schema = @Schema(implementation = LaudoResponse.class))),
            @APIResponse(responseCode = "404", description = "Laudo not found"),
            @APIResponse(responseCode = "422", description = "Laudo is not in DRAFT status")
    })
    public Uni<Response> issue(
            @PathParam("id")
            @Parameter(description = "Laudo UUID") UUID id) {
        return issueLaudoUseCase.execute(id)
                .map(laudo -> Response.ok(LaudoMapper.toResponse(laudo)).build());
    }

    @GET
    @Path("/specialists/{specialistId}/laudos")
    @Operation(summary = "List laudos by specialist", description = "Returns a paginated list of laudos by specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated laudo list")
    })
    public Uni<Response> listBySpecialist(
            @PathParam("specialistId")
            @Parameter(description = "Specialist UUID") UUID specialistId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listLaudosBySpecialistUseCase.execute(specialistId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/appointments/{appointmentId}/laudo")
    @Operation(summary = "Get laudo by appointment", description = "Retrieves the laudo for a given appointment")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Laudo found",
                    content = @Content(schema = @Schema(implementation = LaudoResponse.class))),
            @APIResponse(responseCode = "404", description = "Laudo not found for this appointment")
    })
    public Uni<Response> getByAppointment(
            @PathParam("appointmentId")
            @Parameter(description = "Appointment UUID") UUID appointmentId) {
        return getLaudoByAppointmentUseCase.execute(appointmentId)
                .map(laudo -> Response.ok(LaudoMapper.toResponse(laudo)).build());
    }
}
