package dev.vetra.api.modules.patient.resource;

import dev.vetra.api.modules.patient.dto.CreatePatientRequest;
import dev.vetra.api.modules.patient.dto.PatientMapper;
import dev.vetra.api.modules.patient.dto.PatientResponse;
import dev.vetra.api.modules.patient.dto.UpdatePatientRequest;
import dev.vetra.api.modules.patient.usecase.CreatePatientUseCase;
import dev.vetra.api.modules.patient.usecase.DeletePatientUseCase;
import dev.vetra.api.modules.patient.usecase.GetPatientUseCase;
import dev.vetra.api.modules.patient.usecase.ListPatientsByClinicUseCase;
import dev.vetra.api.modules.patient.usecase.ListPatientsByTutorUseCase;
import dev.vetra.api.modules.patient.usecase.UpdatePatientUseCase;
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
@Tag(name = "Patients", description = "Patient (animal) management endpoints")
public class PatientResource {

    private final CreatePatientUseCase createPatientUseCase;
    private final DeletePatientUseCase deletePatientUseCase;
    private final GetPatientUseCase getPatientUseCase;
    private final ListPatientsByClinicUseCase listPatientsByClinicUseCase;
    private final ListPatientsByTutorUseCase listPatientsByTutorUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;

    @Inject
    public PatientResource(CreatePatientUseCase createPatientUseCase,
                           DeletePatientUseCase deletePatientUseCase,
                           GetPatientUseCase getPatientUseCase,
                           ListPatientsByClinicUseCase listPatientsByClinicUseCase,
                           ListPatientsByTutorUseCase listPatientsByTutorUseCase,
                           UpdatePatientUseCase updatePatientUseCase) {
        this.createPatientUseCase = createPatientUseCase;
        this.deletePatientUseCase = deletePatientUseCase;
        this.getPatientUseCase = getPatientUseCase;
        this.listPatientsByClinicUseCase = listPatientsByClinicUseCase;
        this.listPatientsByTutorUseCase = listPatientsByTutorUseCase;
        this.updatePatientUseCase = updatePatientUseCase;
    }

    @POST
    @Path("/clinics/{clinicId}/tutors/{tutorId}/patients")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Create a new patient", description = "Registers a new patient (animal) for a tutor in a clinic")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Patient created",
                    content = @Content(schema = @Schema(implementation = PatientResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Tutor not found")
    })
    public Uni<Response> create(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @PathParam("tutorId")
            @Parameter(description = "Tutor UUID") UUID tutorId,
            @Valid CreatePatientRequest request) {
        return createPatientUseCase.execute(clinicId, tutorId, request)
                .map(patient -> {
                    PatientResponse body = PatientMapper.toResponse(patient);
                    return Response.created(URI.create("/api/v1/patients/" + patient.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/clinics/{clinicId}/patients")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "List patients by clinic", description = "Returns a paginated list of patients for a clinic")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated patient list")
    })
    public Uni<Response> listByClinic(
            @PathParam("clinicId")
            @Parameter(description = "Clinic UUID") UUID clinicId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listPatientsByClinicUseCase.execute(clinicId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/tutors/{tutorId}/patients")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "List patients by tutor", description = "Returns a paginated list of patients for a tutor")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated patient list")
    })
    public Uni<Response> listByTutor(
            @PathParam("tutorId")
            @Parameter(description = "Tutor UUID") UUID tutorId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listPatientsByTutorUseCase.execute(tutorId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @GET
    @Path("/patients/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF", "SPECIALIST", "PLATFORM_ADMIN"})
    @Operation(summary = "Get patient by ID", description = "Retrieves a single patient by its UUID")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Patient found",
                    content = @Content(schema = @Schema(implementation = PatientResponse.class))),
            @APIResponse(responseCode = "404", description = "Patient not found")
    })
    public Uni<Response> getById(
            @PathParam("id")
            @Parameter(description = "Patient UUID") UUID id) {
        return getPatientUseCase.execute(id)
                .map(patient -> Response.ok(PatientMapper.toResponse(patient)).build());
    }

    @PUT
    @Path("/patients/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Update patient", description = "Updates an existing patient's information")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Patient updated",
                    content = @Content(schema = @Schema(implementation = PatientResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "404", description = "Patient not found")
    })
    public Uni<Response> update(
            @PathParam("id")
            @Parameter(description = "Patient UUID") UUID id,
            @Valid UpdatePatientRequest request) {
        return updatePatientUseCase.execute(id, request)
                .map(patient -> Response.ok(PatientMapper.toResponse(patient)).build());
    }

    @DELETE
    @Path("/patients/{id}")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Delete patient", description = "Deletes a patient. The patient must have no exam requests.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Patient deleted"),
            @APIResponse(responseCode = "404", description = "Patient not found"),
            @APIResponse(responseCode = "422", description = "Patient has exam requests")
    })
    public Uni<Response> delete(@PathParam("id") @Parameter(description = "Patient UUID") UUID id) {
        return deletePatientUseCase.execute(id)
                .map(ignored -> Response.noContent().build());
    }
}
