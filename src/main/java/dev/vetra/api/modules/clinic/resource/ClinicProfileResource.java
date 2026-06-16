package dev.vetra.api.modules.clinic.resource;

import dev.vetra.api.modules.clinic.dto.ClinicMapper;
import dev.vetra.api.modules.clinic.dto.ClinicResponse;
import dev.vetra.api.modules.clinic.dto.ClinicStaffMapper;
import dev.vetra.api.modules.clinic.dto.ClinicStaffResponse;
import dev.vetra.api.modules.clinic.dto.UpdateClinicProfileRequest;
import dev.vetra.api.modules.clinic.dto.UpdateMyStaffProfileRequest;
import dev.vetra.api.modules.clinic.usecase.GetMyClinicProfileUseCase;
import dev.vetra.api.modules.clinic.usecase.GetMyStaffProfileUseCase;
import dev.vetra.api.modules.clinic.usecase.UpdateClinicProfileUseCase;
import dev.vetra.api.modules.clinic.usecase.UpdateMyStaffProfileUseCase;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Clinic Profile", description = "Clinic self-service profile management endpoints")
public class ClinicProfileResource {

    private static final Logger LOG = Logger.getLogger(ClinicProfileResource.class);

    private final SecurityContext securityContext;
    private final GetMyClinicProfileUseCase getMyClinicProfileUseCase;
    private final UpdateClinicProfileUseCase updateClinicProfileUseCase;
    private final GetMyStaffProfileUseCase getMyStaffProfileUseCase;
    private final UpdateMyStaffProfileUseCase updateMyStaffProfileUseCase;

    @Inject
    public ClinicProfileResource(SecurityContext securityContext,
                                 GetMyClinicProfileUseCase getMyClinicProfileUseCase,
                                 UpdateClinicProfileUseCase updateClinicProfileUseCase,
                                 GetMyStaffProfileUseCase getMyStaffProfileUseCase,
                                 UpdateMyStaffProfileUseCase updateMyStaffProfileUseCase) {
        this.securityContext = securityContext;
        this.getMyClinicProfileUseCase = getMyClinicProfileUseCase;
        this.updateClinicProfileUseCase = updateClinicProfileUseCase;
        this.getMyStaffProfileUseCase = getMyStaffProfileUseCase;
        this.updateMyStaffProfileUseCase = updateMyStaffProfileUseCase;
    }

    // ---- Clinic Profile ----

    @GET
    @Path("/clinic/my-profile")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Get own clinic profile", description = "Retrieves the clinic profile for the currently authenticated clinic user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic profile found",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — CLINIC_ADMIN or CLINIC_STAFF role required"),
            @APIResponse(responseCode = "404", description = "Clinic profile not found for current user")
    })
    public Uni<Response> getClinicProfile() {
        String userId = extractUserId();
        LOG.debugf("GET clinic profile for userId=%s", userId);
        return getMyClinicProfileUseCase.execute(userId)
                .map(clinic -> Response.ok(ClinicMapper.toResponse(clinic)).build());
    }

    @PUT
    @Path("/clinic/my-profile")
    @RolesAllowed("CLINIC_ADMIN")
    @Operation(summary = "Update own clinic profile", description = "Updates the clinic profile. Only CLINIC_ADMIN can update clinic information. Identity fields (document, email) cannot be changed.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Clinic profile updated",
                    content = @Content(schema = @Schema(implementation = ClinicResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — CLINIC_ADMIN role required"),
            @APIResponse(responseCode = "404", description = "Clinic profile not found for current user")
    })
    public Uni<Response> updateClinicProfile(@Valid UpdateClinicProfileRequest request) {
        String userId = extractUserId();
        LOG.debugf("PUT clinic profile for userId=%s", userId);
        return updateClinicProfileUseCase.execute(userId, request)
                .map(clinic -> Response.ok(ClinicMapper.toResponse(clinic)).build());
    }

    // ---- Staff Profile ----

    @GET
    @Path("/staff/my-profile")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Get own staff profile", description = "Retrieves the staff profile for the currently authenticated clinic user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Staff profile found",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — CLINIC_ADMIN or CLINIC_STAFF role required"),
            @APIResponse(responseCode = "404", description = "Staff profile not found for current user")
    })
    public Uni<Response> getStaffProfile() {
        String userId = extractUserId();
        LOG.debugf("GET staff profile for userId=%s", userId);
        return getMyStaffProfileUseCase.execute(userId)
                .map(staff -> Response.ok(ClinicStaffMapper.toResponse(staff)).build());
    }

    @PUT
    @Path("/staff/my-profile")
    @RolesAllowed({"CLINIC_ADMIN", "CLINIC_STAFF"})
    @Operation(summary = "Update own staff profile", description = "Updates the staff profile for the currently authenticated clinic user. Only name and phone can be changed.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Staff profile updated",
                    content = @Content(schema = @Schema(implementation = ClinicStaffResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error"),
            @APIResponse(responseCode = "401", description = "Unauthorized"),
            @APIResponse(responseCode = "403", description = "Forbidden — CLINIC_ADMIN or CLINIC_STAFF role required"),
            @APIResponse(responseCode = "404", description = "Staff profile not found for current user")
    })
    public Uni<Response> updateStaffProfile(@Valid UpdateMyStaffProfileRequest request) {
        String userId = extractUserId();
        LOG.debugf("PUT staff profile for userId=%s", userId);
        return updateMyStaffProfileUseCase.execute(userId, request)
                .map(staff -> Response.ok(ClinicStaffMapper.toResponse(staff)).build());
    }

    private String extractUserId() {
        return securityContext.userId()
                .orElseThrow(() -> new SecurityException("Unable to extract user ID from security context"));
    }
}
