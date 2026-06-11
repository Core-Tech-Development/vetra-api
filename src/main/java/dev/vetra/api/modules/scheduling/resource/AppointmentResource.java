package dev.vetra.api.modules.scheduling.resource;

import dev.vetra.api.modules.scheduling.dto.AppointmentMapper;
import dev.vetra.api.modules.scheduling.dto.AppointmentNoteResponse;
import dev.vetra.api.modules.scheduling.dto.AppointmentResponse;
import dev.vetra.api.modules.scheduling.dto.CancelAppointmentRequest;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentNoteRequest;
import dev.vetra.api.modules.scheduling.dto.CreateAppointmentRequest;
import dev.vetra.api.modules.scheduling.usecase.AcceptAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.CancelAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.CreateAppointmentNoteUseCase;
import dev.vetra.api.modules.scheduling.usecase.DeclineAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.CompleteAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.CompleteExamUseCase;
import dev.vetra.api.modules.scheduling.usecase.GetAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.ListAppointmentNotesByAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.ListAppointmentsBySpecialistUseCase;
import dev.vetra.api.modules.scheduling.usecase.ListAppointmentsByStatusUseCase;
import dev.vetra.api.modules.scheduling.usecase.NoShowAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.ScheduleAppointmentUseCase;
import dev.vetra.api.modules.scheduling.usecase.StartServiceUseCase;
import dev.vetra.api.modules.scheduling.usecase.StartTransitUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
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
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentResource {

    private final ScheduleAppointmentUseCase scheduleAppointmentUseCase;
    private final GetAppointmentUseCase getAppointmentUseCase;
    private final ListAppointmentsByStatusUseCase listAppointmentsByStatusUseCase;
    private final ListAppointmentsBySpecialistUseCase listAppointmentsBySpecialistUseCase;
    private final AcceptAppointmentUseCase acceptAppointmentUseCase;
    private final StartTransitUseCase startTransitUseCase;
    private final StartServiceUseCase startServiceUseCase;
    private final CompleteExamUseCase completeExamUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;
    private final NoShowAppointmentUseCase noShowAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final DeclineAppointmentUseCase declineAppointmentUseCase;
    private final CreateAppointmentNoteUseCase createAppointmentNoteUseCase;
    private final ListAppointmentNotesByAppointmentUseCase listAppointmentNotesByAppointmentUseCase;
    private final SecurityContext securityContext;

    @Inject
    public AppointmentResource(ScheduleAppointmentUseCase scheduleAppointmentUseCase,
                               GetAppointmentUseCase getAppointmentUseCase,
                               ListAppointmentsByStatusUseCase listAppointmentsByStatusUseCase,
                               ListAppointmentsBySpecialistUseCase listAppointmentsBySpecialistUseCase,
                               AcceptAppointmentUseCase acceptAppointmentUseCase,
                               StartTransitUseCase startTransitUseCase,
                               StartServiceUseCase startServiceUseCase,
                               CompleteExamUseCase completeExamUseCase,
                               CompleteAppointmentUseCase completeAppointmentUseCase,
                               NoShowAppointmentUseCase noShowAppointmentUseCase,
                               CancelAppointmentUseCase cancelAppointmentUseCase,
                               DeclineAppointmentUseCase declineAppointmentUseCase,
                               CreateAppointmentNoteUseCase createAppointmentNoteUseCase,
                               ListAppointmentNotesByAppointmentUseCase listAppointmentNotesByAppointmentUseCase,
                               SecurityContext securityContext) {
        this.scheduleAppointmentUseCase = scheduleAppointmentUseCase;
        this.getAppointmentUseCase = getAppointmentUseCase;
        this.listAppointmentsByStatusUseCase = listAppointmentsByStatusUseCase;
        this.listAppointmentsBySpecialistUseCase = listAppointmentsBySpecialistUseCase;
        this.acceptAppointmentUseCase = acceptAppointmentUseCase;
        this.startTransitUseCase = startTransitUseCase;
        this.startServiceUseCase = startServiceUseCase;
        this.completeExamUseCase = completeExamUseCase;
        this.completeAppointmentUseCase = completeAppointmentUseCase;
        this.noShowAppointmentUseCase = noShowAppointmentUseCase;
        this.cancelAppointmentUseCase = cancelAppointmentUseCase;
        this.declineAppointmentUseCase = declineAppointmentUseCase;
        this.createAppointmentNoteUseCase = createAppointmentNoteUseCase;
        this.listAppointmentNotesByAppointmentUseCase = listAppointmentNotesByAppointmentUseCase;
        this.securityContext = securityContext;
    }

    @POST
    @Path("/appointments")
    @Operation(summary = "Schedule appointment",
            description = "Creates a new appointment. If an availability slot ID is provided, the slot is reserved.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Appointment created",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Availability slot not found"),
            @APIResponse(responseCode = "422", description = "Slot not available")
    })
    public Uni<Response> schedule(@Valid CreateAppointmentRequest request) {
        return scheduleAppointmentUseCase.execute(request)
                .map(appointment -> {
                    AppointmentResponse body = AppointmentMapper.toResponse(appointment);
                    return Response.created(URI.create("/api/v1/appointments/" + appointment.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/appointments/{id}")
    @Operation(summary = "Get appointment by ID",
            description = "Retrieves a single appointment by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment found",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        return getAppointmentUseCase.execute(id)
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @GET
    @Path("/appointments")
    @Operation(summary = "List appointments by status",
            description = "Returns a paginated list of appointments filtered by status")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated appointment list"),
            @APIResponse(responseCode = "400", description = "Invalid status")
    })
    public Uni<Response> listByStatus(
            @QueryParam("status")
            @Parameter(description = "Appointment status filter") String status,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listAppointmentsByStatusUseCase.execute(status, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/specialists/{specialistId}/appointments")
    @Operation(summary = "List appointments by specialist",
            description = "Returns a paginated list of appointments for a specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated appointment list")
    })
    public Uni<Response> listBySpecialist(
            @PathParam("specialistId")
            @Parameter(description = "Specialist UUID") UUID specialistId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listAppointmentsBySpecialistUseCase.execute(specialistId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @PATCH
    @Path("/appointments/{id}/accept")
    @Operation(summary = "Accept appointment",
            description = "Specialist accepts the appointment. Transitions from WAITING_SPECIALIST_ACCEPTANCE to ACCEPTED.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment accepted",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> accept(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        String callerId = securityContext.userId().orElse(null);
        return acceptAppointmentUseCase.execute(id, callerId, securityContext.roles())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/start-transit")
    @Operation(summary = "Start transit",
            description = "Specialist starts transit to the clinic. Transitions from ACCEPTED/SCHEDULED to IN_TRANSIT.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Transit started",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> startTransit(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        String callerId = securityContext.userId().orElse(null);
        return startTransitUseCase.execute(id, callerId, securityContext.roles())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/start-service")
    @Operation(summary = "Start service",
            description = "Specialist starts the exam service at the clinic. Transitions from IN_TRANSIT to IN_SERVICE and records actual start time.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Service started",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> startService(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        String callerId = securityContext.userId().orElse(null);
        return startServiceUseCase.execute(id, callerId, securityContext.roles())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/complete-exam")
    @Operation(summary = "Complete exam",
            description = "Specialist completes the exam. Transitions from IN_SERVICE to WAITING_REPORT and records actual end time.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Exam completed",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> completeExam(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        String callerId = securityContext.userId().orElse(null);
        return completeExamUseCase.execute(id, callerId, securityContext.roles())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/complete")
    @Operation(summary = "Complete appointment",
            description = "Completes an appointment after the report has been issued. Transitions from REPORT_ISSUED to COMPLETED.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment completed",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> complete(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        return completeAppointmentUseCase.execute(id)
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/no-show")
    @Operation(summary = "Mark as no-show",
            description = "Marks an appointment as no-show. Valid from ACCEPTED or IN_TRANSIT. If a slot was reserved, it is freed.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment marked as no-show",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Invalid status transition")
    })
    public Uni<Response> noShow(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        return noShowAppointmentUseCase.execute(id)
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/decline")
    @Operation(summary = "Decline appointment",
            description = "Specialist declines an appointment request. Only valid from WAITING_SPECIALIST_ACCEPTANCE. Reverts exam request to CREATED.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment declined",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Can only decline from WAITING_SPECIALIST_ACCEPTANCE")
    })
    public Uni<Response> decline(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id,
            CancelAppointmentRequest request) {
        String reason = request != null ? request.reason() : null;
        String callerId = securityContext.userId().orElse(null);
        return declineAppointmentUseCase.execute(id, reason, callerId, securityContext.roles())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    @PATCH
    @Path("/appointments/{id}/cancel")
    @Operation(summary = "Cancel appointment",
            description = "Cancels an appointment with a reason. If a slot was reserved, it is freed back to AVAILABLE.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Appointment cancelled",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "422", description = "Cannot cancel appointment with current status")
    })
    public Uni<Response> cancel(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id,
            @Valid CancelAppointmentRequest request) {
        return cancelAppointmentUseCase.execute(id, request.reason())
                .map(appointment -> Response.ok(AppointmentMapper.toResponse(appointment)).build());
    }

    // ---- Appointment Notes ----

    @POST
    @Path("/appointments/{id}/notes")
    @Operation(summary = "Create appointment note",
            description = "Adds a note (observation or incident record) to an appointment.")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Note created",
                    content = @Content(schema = @Schema(implementation = AppointmentNoteResponse.class))),
            @APIResponse(responseCode = "404", description = "Appointment not found"),
            @APIResponse(responseCode = "400", description = "Validation error")
    })
    public Uni<Response> createNote(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id,
            @Valid CreateAppointmentNoteRequest request) {
        String authorUserId = securityContext.userId().orElse("unknown");
        return createAppointmentNoteUseCase.execute(id, authorUserId, request.title(), request.content())
                .map(note -> {
                    var response = new AppointmentNoteResponse(
                            note.id(), note.appointmentId(), note.authorUserId(),
                            note.title(), note.content(), note.createdAt());
                    return Response.created(URI.create("/api/v1/appointments/" + id + "/notes/" + note.id()))
                            .entity(response)
                            .build();
                });
    }

    @GET
    @Path("/appointments/{id}/notes")
    @Operation(summary = "List appointment notes",
            description = "Returns all notes for an appointment in chronological order.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of notes")
    })
    public Uni<Response> listNotes(
            @PathParam("id")
            @Parameter(description = "Appointment UUID") UUID id) {
        return listAppointmentNotesByAppointmentUseCase.execute(id)
                .map(notes -> {
                    List<AppointmentNoteResponse> responses = notes.stream()
                            .map(note -> new AppointmentNoteResponse(
                                    note.id(), note.appointmentId(), note.authorUserId(),
                                    note.title(), note.content(), note.createdAt()))
                            .toList();
                    return Response.ok(responses).build();
                });
    }
}
