package dev.vetra.api.modules.scheduling.resource;

import dev.vetra.api.modules.scheduling.dto.CreateSlotRequest;
import dev.vetra.api.modules.scheduling.dto.SlotMapper;
import dev.vetra.api.modules.scheduling.dto.SlotResponse;
import dev.vetra.api.modules.scheduling.usecase.BlockSlotUseCase;
import dev.vetra.api.modules.scheduling.usecase.CreateSlotUseCase;
import dev.vetra.api.modules.scheduling.usecase.DeleteSlotUseCase;
import dev.vetra.api.modules.scheduling.usecase.ListSlotsUseCase;
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
@Tag(name = "Availability Slots", description = "Specialist availability slot management endpoints")
public class AvailabilitySlotResource {

    private final CreateSlotUseCase createSlotUseCase;
    private final ListSlotsUseCase listSlotsUseCase;
    private final DeleteSlotUseCase deleteSlotUseCase;
    private final BlockSlotUseCase blockSlotUseCase;

    @Inject
    public AvailabilitySlotResource(CreateSlotUseCase createSlotUseCase,
                                    ListSlotsUseCase listSlotsUseCase,
                                    DeleteSlotUseCase deleteSlotUseCase,
                                    BlockSlotUseCase blockSlotUseCase) {
        this.createSlotUseCase = createSlotUseCase;
        this.listSlotsUseCase = listSlotsUseCase;
        this.deleteSlotUseCase = deleteSlotUseCase;
        this.blockSlotUseCase = blockSlotUseCase;
    }

    @POST
    @Path("/specialists/{specialistId}/availability-slots")
    @Operation(summary = "Create availability slot",
            description = "Creates a new availability slot for a specialist")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Slot created",
                    content = @Content(schema = @Schema(implementation = SlotResponse.class))),
            @APIResponse(responseCode = "400", description = "Validation error")
    })
    public Uni<Response> create(
            @PathParam("specialistId")
            @Parameter(description = "Specialist UUID") UUID specialistId,
            @Valid CreateSlotRequest request) {
        return createSlotUseCase.execute(specialistId, request)
                .map(slot -> {
                    SlotResponse body = SlotMapper.toResponse(slot);
                    return Response.created(
                                    URI.create("/api/v1/availability-slots/" + slot.id()))
                            .entity(body)
                            .build();
                });
    }

    @GET
    @Path("/specialists/{specialistId}/availability-slots")
    @Operation(summary = "List availability slots",
            description = "Returns a paginated list of availability slots for a specialist")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated slot list")
    })
    public Uni<Response> list(
            @PathParam("specialistId")
            @Parameter(description = "Specialist UUID") UUID specialistId,
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listSlotsUseCase.execute(specialistId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @DELETE
    @Path("/availability-slots/{id}")
    @Operation(summary = "Delete availability slot",
            description = "Deletes an availability slot. Only AVAILABLE slots can be deleted.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Slot deleted"),
            @APIResponse(responseCode = "404", description = "Slot not found"),
            @APIResponse(responseCode = "422", description = "Slot cannot be deleted due to its status")
    })
    public Uni<Response> delete(
            @PathParam("id")
            @Parameter(description = "Slot UUID") UUID id) {
        return deleteSlotUseCase.execute(id)
                .map(ignored -> Response.noContent().build());
    }

    @PATCH
    @Path("/availability-slots/{id}/block")
    @Operation(summary = "Block availability slot",
            description = "Blocks an availability slot. Only AVAILABLE slots can be blocked.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Slot blocked",
                    content = @Content(schema = @Schema(implementation = SlotResponse.class))),
            @APIResponse(responseCode = "404", description = "Slot not found"),
            @APIResponse(responseCode = "422", description = "Slot cannot be blocked due to its status")
    })
    public Uni<Response> block(
            @PathParam("id")
            @Parameter(description = "Slot UUID") UUID id) {
        return blockSlotUseCase.execute(id)
                .map(slot -> Response.ok(SlotMapper.toResponse(slot)).build());
    }
}
