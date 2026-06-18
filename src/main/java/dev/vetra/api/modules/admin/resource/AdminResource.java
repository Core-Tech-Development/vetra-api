package dev.vetra.api.modules.admin.resource;

import dev.vetra.api.modules.admin.dto.AdminDashboardResponse;
import dev.vetra.api.modules.admin.usecase.ApproveClinicUseCase;
import dev.vetra.api.modules.admin.usecase.GetAdminDashboardUseCase;
import dev.vetra.api.modules.admin.usecase.SuspendClinicUseCase;
import dev.vetra.api.modules.billing.usecase.DeleteBillingRecordUseCase;
import dev.vetra.api.modules.clinic.dto.ClinicMapper;
import dev.vetra.api.modules.clinic.dto.ClinicResponse;
import dev.vetra.api.modules.clinic.usecase.DeleteClinicUseCase;
import dev.vetra.api.modules.specialist.usecase.DeleteSpecialistUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
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

import java.util.UUID;

@Path("/api/v1/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Platform administration endpoints")
public class AdminResource {

    private final ApproveClinicUseCase approveClinicUseCase;
    private final SuspendClinicUseCase suspendClinicUseCase;
    private final GetAdminDashboardUseCase getAdminDashboardUseCase;
    private final DeleteClinicUseCase deleteClinicUseCase;
    private final DeleteSpecialistUseCase deleteSpecialistUseCase;
    private final DeleteBillingRecordUseCase deleteBillingRecordUseCase;

    @Inject
    public AdminResource(ApproveClinicUseCase approveClinicUseCase,
                          SuspendClinicUseCase suspendClinicUseCase,
                          GetAdminDashboardUseCase getAdminDashboardUseCase,
                          DeleteClinicUseCase deleteClinicUseCase,
                          DeleteSpecialistUseCase deleteSpecialistUseCase,
                          DeleteBillingRecordUseCase deleteBillingRecordUseCase) {
        this.approveClinicUseCase = approveClinicUseCase;
        this.suspendClinicUseCase = suspendClinicUseCase;
        this.getAdminDashboardUseCase = getAdminDashboardUseCase;
        this.deleteClinicUseCase = deleteClinicUseCase;
        this.deleteSpecialistUseCase = deleteSpecialistUseCase;
        this.deleteBillingRecordUseCase = deleteBillingRecordUseCase;
    }

    @PATCH
    @Path("/clinics/{id}/approve")
    @Operation(summary = "Approve a clinic", description = "Approves a clinic with PENDING_APPROVAL status, changing it to ACTIVE")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic approved",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "404", description = "Clinic not found"),
            @APIResponse(responseCode = "422", description = "Invalid clinic status for approval")
    })
    public Uni<Response> approveClinic(
            @PathParam("id")
            @Parameter(description = "Clinic UUID") UUID id) {
        return approveClinicUseCase.execute(id)
                .map(clinic -> Response.ok(ClinicMapper.toResponse(clinic)).build());
    }

    @PATCH
    @Path("/clinics/{id}/suspend")
    @Operation(summary = "Suspend a clinic", description = "Suspends an ACTIVE clinic, changing it to SUSPENDED")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic suspended",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "404", description = "Clinic not found"),
            @APIResponse(responseCode = "422", description = "Invalid clinic status for suspension")
    })
    public Uni<Response> suspendClinic(
            @PathParam("id")
            @Parameter(description = "Clinic UUID") UUID id) {
        return suspendClinicUseCase.execute(id)
                .map(clinic -> Response.ok(ClinicMapper.toResponse(clinic)).build());
    }

    @GET
    @Path("/dashboard")
    @Operation(summary = "Get admin dashboard", description = "Returns platform-wide statistics for the admin dashboard")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Dashboard statistics",
                    content = @Content(schema = @Schema(implementation = AdminDashboardResponse.class)))
    })
    public Uni<Response> getDashboard() {
        return getAdminDashboardUseCase.execute()
                .map(dashboard -> Response.ok(dashboard).build());
    }

    @DELETE
    @Path("/clinics/{id}")
    @RolesAllowed("PLATFORM_ADMIN")
    @Operation(summary = "Delete a clinic", description = "Deletes a clinic if it has no dependent records")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Clinic deleted"),
            @APIResponse(responseCode = "404", description = "Clinic not found"),
            @APIResponse(responseCode = "409", description = "Clinic has dependent records")
    })
    public Uni<Response> deleteClinic(
            @PathParam("id")
            @Parameter(description = "Clinic UUID") UUID id) {
        return deleteClinicUseCase.execute(id)
                .replaceWith(Response.noContent().build());
    }

    @DELETE
    @Path("/specialists/{id}")
    @RolesAllowed("PLATFORM_ADMIN")
    @Operation(summary = "Delete a specialist", description = "Deletes a specialist if they have no active appointments")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Specialist deleted"),
            @APIResponse(responseCode = "404", description = "Specialist not found"),
            @APIResponse(responseCode = "409", description = "Specialist has dependent records")
    })
    public Uni<Response> deleteSpecialist(
            @PathParam("id")
            @Parameter(description = "Specialist UUID") UUID id) {
        return deleteSpecialistUseCase.execute(id)
                .replaceWith(Response.noContent().build());
    }

    @DELETE
    @Path("/billing-records/{id}")
    @RolesAllowed("PLATFORM_ADMIN")
    @Operation(summary = "Delete a billing record", description = "Deletes a billing record")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Billing record deleted"),
            @APIResponse(responseCode = "404", description = "Billing record not found")
    })
    public Uni<Response> deleteBillingRecord(
            @PathParam("id")
            @Parameter(description = "Billing record UUID") UUID id) {
        return deleteBillingRecordUseCase.execute(id)
                .replaceWith(Response.noContent().build());
    }
}
