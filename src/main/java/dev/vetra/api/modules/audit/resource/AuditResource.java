package dev.vetra.api.modules.audit.resource;

import dev.vetra.api.modules.audit.usecase.ListAuditLogsUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin/audit-logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Audit", description = "Audit log endpoints")
public class AuditResource {

    private final ListAuditLogsUseCase listAuditLogsUseCase;

    @Inject
    public AuditResource(ListAuditLogsUseCase listAuditLogsUseCase) {
        this.listAuditLogsUseCase = listAuditLogsUseCase;
    }

    @GET
    @Operation(summary = "List audit logs", description = "Returns a paginated list of audit log entries")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated audit log list")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        return listAuditLogsUseCase.execute(pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }
}
