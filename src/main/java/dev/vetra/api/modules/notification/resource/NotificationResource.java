package dev.vetra.api.modules.notification.resource;

import dev.vetra.api.modules.notification.usecase.CountUnreadNotificationsUseCase;
import dev.vetra.api.modules.notification.usecase.ListNotificationsUseCase;
import dev.vetra.api.modules.notification.usecase.MarkAllNotificationsReadUseCase;
import dev.vetra.api.modules.notification.usecase.MarkNotificationReadUseCase;
import dev.vetra.api.shared.pagination.PageRequest;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications", description = "User notification endpoints")
public class NotificationResource {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final CountUnreadNotificationsUseCase countUnreadNotificationsUseCase;
    private final SecurityContext securityContext;

    @Inject
    public NotificationResource(ListNotificationsUseCase listNotificationsUseCase,
                                 MarkNotificationReadUseCase markNotificationReadUseCase,
                                 MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase,
                                 CountUnreadNotificationsUseCase countUnreadNotificationsUseCase,
                                 SecurityContext securityContext) {
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.markAllNotificationsReadUseCase = markAllNotificationsReadUseCase;
        this.countUnreadNotificationsUseCase = countUnreadNotificationsUseCase;
        this.securityContext = securityContext;
    }

    @GET
    @Operation(summary = "List notifications", description = "Returns a paginated list of notifications for the current user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Paginated notification list"),
            @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> list(
            @QueryParam("page") @DefaultValue("0")
            @Parameter(description = "Page number (0-based)") Integer page,
            @QueryParam("size") @DefaultValue("20")
            @Parameter(description = "Page size (max 100)") Integer size) {

        String userId = securityContext.userId()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        PageRequest pageRequest = PageRequest.of(page, size);
        return listNotificationsUseCase.execute(userId, pageRequest)
                .map(pageResponse -> Response.ok(pageResponse).build());
    }

    @PATCH
    @Path("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Notification marked as read"),
            @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> markAsRead(
            @PathParam("id") @Parameter(description = "Notification ID") UUID id) {

        securityContext.userId()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return markNotificationReadUseCase.execute(id)
                .map(ignored -> Response.noContent().build());
    }

    @POST
    @Path("/mark-all-read")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the current user")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "All notifications marked as read"),
            @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> markAllAsRead() {

        String userId = securityContext.userId()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return markAllNotificationsReadUseCase.execute(userId)
                .map(ignored -> Response.noContent().build());
    }

    @GET
    @Path("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Returns the number of unread notifications for the current user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Unread count"),
            @APIResponse(responseCode = "401", description = "Unauthorized")
    })
    public Uni<Response> unreadCount() {

        String userId = securityContext.userId()
                .orElseThrow(() -> new SecurityException("User not authenticated"));

        return countUnreadNotificationsUseCase.execute(userId)
                .map(count -> Response.ok(Map.of("count", count)).build());
    }
}
