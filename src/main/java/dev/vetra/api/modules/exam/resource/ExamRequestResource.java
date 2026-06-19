package dev.vetra.api.modules.exam.resource;

import dev.vetra.api.modules.exam.dto.CreateExamRequestRequest;
import dev.vetra.api.modules.exam.dto.ExamRequestMapper;
import dev.vetra.api.modules.exam.dto.ExamRequestResponse;
import dev.vetra.api.modules.exam.dto.UpdateExamRequestRequest;
import dev.vetra.api.modules.exam.usecase.CancelExamRequestUseCase;
import dev.vetra.api.modules.exam.usecase.CreateExamRequestUseCase;
import dev.vetra.api.modules.exam.usecase.DeleteExamRequestUseCase;
import dev.vetra.api.modules.exam.usecase.GetExamRequestUseCase;
import dev.vetra.api.modules.exam.usecase.ListExamRequestsByClinicUseCase;
import dev.vetra.api.modules.exam.usecase.ListExamRequestsByPatientUseCase;
import dev.vetra.api.modules.exam.usecase.UpdateExamRequestUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.pagination.PageResponse;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
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
@Tag(name = "Exam Requests", description = "Exam request management endpoints")
public class ExamRequestResource {

    private final CreateExamRequestUseCase createExamRequestUseCase;
    private final GetExamRequestUseCase getExamRequestUseCase;
    private final ListExamRequestsByClinicUseCase listExamRequestsByClinicUseCase;
    private final ListExamRequestsByPatientUseCase listExamRequestsByPatientUseCase;
    private final CancelExamRequestUseCase cancelExamRequestUseCase;
    private final UpdateExamRequestUseCase updateExamRequestUseCase;
    private final DeleteExamRequestUseCase deleteExamRequestUseCase;
    private final SecurityContext securityContext;

    @Inject
    public ExamRequestResource(CreateExamRequestUseCase createExamRequestUseCase,
                               GetExamRequestUseCase getExamRequestUseCase,
                               ListExamRequestsByClinicUseCase listExamRequestsByClinicUseCase,
                               ListExamRequestsByPatientUseCase listExamRequestsByPatientUseCase,
                               CancelExamRequestUseCase cancelExamRequestUseCase,
                               UpdateExamRequestUseCase updateExamRequestUseCase,
                               DeleteExamRequestUseCase deleteExamRequestUseCase,
                               SecurityContext securityContext) {
        this.createExamRequestUseCase = createExamRequestUseCase;
        this.getExamRequestUseCase = getExamRequestUseCase;
        this.listExamRequestsByClinicUseCase = listExamRequestsByClinicUseCase;
        this.listExamRequestsByPatientUseCase = listExamRequestsByPatientUseCase;
        this.cancelExamRequestUseCase = cancelExamRequestUseCase;
        this.updateExamRequestUseCase = updateExamRequestUseCase;
        this.deleteExamRequestUseCase = deleteExamRequestUseCase;
        this.securityContext = securityContext;
    }

    @POST
    @Path("/clinics/{clinicId}/exam-requests")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Create a new exam request", description = "Creates a new exam request for a patient in a clinic")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Exam request created",
                    content = @Content(schema = @Schema(implementation = ExamRequestResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Patient not found")
    })
    public Uni<Response> create(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @Valid CreateExamRequestRequest request) {
        String requestedBy = securityContext.userId().orElse("anonymous");
        return createExamRequestUseCase.execute(clinicId, request, requestedBy)
                .map(examRequest -> {
                    ExamRequestResponse body = ExamRequestMapper.toResponse(examRequest);
                    return Response.created(URI.create("/api/v1/exam-requests/" + examRequest.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/clinics/{clinicId}/exam-requests")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "List exam requests by clinic", description = "Returns a paginated list of exam requests for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated exam request list")
    })
    public Uni<Response> listByClinic(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listExamRequestsByClinicUseCase.execute(clinicId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/exam-requests/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF", "SPECIALIST", "PLATFORM_ADMIN"})
    @Operation(summary = "Get exam request by ID", description = "Retrieves a single exam request by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Exam request found",
                    content = @Content(schema = @Schema(implementation = ExamRequestResponse.class))),
            @APIResponse(responseCode = "404", description = "Exam request not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Exam request UUID") UUID id) {
        return getExamRequestUseCase.execute(id)
                .map(examRequest -> Response.ok(ExamRequestMapper.toResponse(examRequest)).build());
    }

    @PUT
    @Path("/exam-requests/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Update exam request", description = "Updates an existing exam request (only if in CREATED status)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Exam request updated",
                    content = @Content(schema = @Schema(implementation = ExamRequestResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error or invalid status"),
            @APIResponse(responseCode = "404", description = "Exam request not found")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Exam request UUID") UUID id,
            @Valid UpdateExamRequestRequest request) {
        return updateExamRequestUseCase.execute(id, request)
                .map(examRequest -> Response.ok(ExamRequestMapper.toResponse(examRequest)).build());
    }

    @PATCH
    @Path("/exam-requests/{id}/cancel")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Cancel exam request", description = "Cancels an exam request (only if in CREATED or PENDING_SPECIALIST status)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Exam request cancelled",
                    content = @Content(schema = @Schema(implementation = ExamRequestResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid status for cancellation"),
            @APIResponse(responseCode = "404", description = "Exam request not found")
    })
    public Uni<Response> cancel(
            @PathParam("id")
            @Parameter(description = "Exam request UUID") UUID id) {
        return cancelExamRequestUseCase.execute(id)
                .map(examRequest -> Response.ok(ExamRequestMapper.toResponse(examRequest)).build());
    }

    @GET
    @Path("/patients/{patientId}/exam-requests")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "List exam requests by patient", description = "Returns all exam requests for a given patient")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Exam request list")
    })
    public Uni<Response> listByPatient(
            @PathParam("patientId")
            @Parameter(description = "Patient UUID") UUID patientId) {
        return listExamRequestsByPatientUseCase.execute(patientId)
                .map(requests -> Response.ok(requests).build());
    }

    @DELETE
    @Path("/exam-requests/{id}")
    @RolesAllowed("PLATFORM_ADMIN")
    @Operation(summary = "Delete exam request", description = "Deletes an exam request if it has no dependent appointments")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Exam request deleted"),
            @APIResponse(responseCode = "404", description = "Exam request not found"),
            @APIResponse(responseCode = "409", description = "Exam request has dependent records")
    })
    public Uni<Response> delete(
            @PathParam("id")
            @Parameter(description = "Exam request UUID") UUID id) {
        return deleteExamRequestUseCase.execute(id)
                .replaceWith(Response.noContent().build());
    }
}
