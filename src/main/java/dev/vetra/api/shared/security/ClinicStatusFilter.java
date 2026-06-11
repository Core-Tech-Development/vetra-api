package dev.vetra.api.shared.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Request filter that blocks API access for clinic users whose clinic
 * is not in ACTIVE status (e.g. PENDING_APPROVAL, SUSPENDED).
 */
@ApplicationScoped
public class ClinicStatusFilter {

    private static final Logger LOG = Logger.getLogger(ClinicStatusFilter.class);

    private final PgPool client;
    private final SecurityIdentity identity;

    @Inject
    public ClinicStatusFilter(PgPool client, SecurityIdentity identity) {
        this.client = client;
        this.identity = identity;
    }

    @ServerRequestFilter
    public Uni<Response> checkClinicStatus(ContainerRequestContext ctx) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().nullItem();
        }

        boolean isClinicUser = identity.hasRole("CLINIC_ADMIN")
                || identity.hasRole("CLINIC_STAFF");
        if (!isClinicUser) {
            return Uni.createFrom().nullItem();
        }

        // Allow clinic lookup by email — needed for login resolution
        String path = ctx.getUriInfo().getPath();
        if (path.contains("/clinics/by-email")) {
            return Uni.createFrom().nullItem();
        }

        String email = extractEmail();
        if (email == null) {
            return Uni.createFrom().nullItem();
        }

        return client.preparedQuery("SELECT status FROM clinic WHERE email = $1")
                .execute(Tuple.of(email))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        return null;
                    }
                    String status = rows.iterator().next().getString("status");
                    if ("ACTIVE".equals(status)) {
                        return null;
                    }
                    LOG.warnf("Blocked request from clinic user %s — clinic status: %s", email, status);
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\":\"CLINIC_NOT_ACTIVE\",\"message\":\"Clinic must be approved before accessing the platform\"}")
                            .type("application/json")
                            .build();
                });
    }

    private String extractEmail() {
        var principal = identity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            return jwt.getClaim("email");
        }
        return null;
    }
}
