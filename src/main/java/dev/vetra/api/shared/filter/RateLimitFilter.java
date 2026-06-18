package dev.vetra.api.shared.filter;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-level rate limiter per authenticated user.
 * <p>
 * Limits each user to {@value MAX_REQUESTS_PER_MINUTE} requests per minute.
 * Anonymous requests are not rate-limited here (nginx handles IP-based limiting).
 * <p>
 * Uses an in-memory ConcurrentHashMap with a scheduled counter reset every minute.
 * This is suitable for single-instance deployments. For horizontal scaling,
 * replace with a Redis-based sliding window counter.
 */
@Singleton
public class RateLimitFilter {

    private static final Logger LOG = Logger.getLogger(RateLimitFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 120;

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Inject
    SecurityIdentity identity;

    @ServerRequestFilter
    public Uni<Response> filter(ContainerRequestContext ctx) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().nullItem();
        }

        String userId = identity.getPrincipal().getName();
        AtomicInteger counter = counters.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > MAX_REQUESTS_PER_MINUTE) {
            LOG.warnf("Rate limit exceeded for user %s: %d req/min", userId, count);
            return Uni.createFrom().item(
                    Response.status(429)
                            .header("Retry-After", "60")
                            .header("Content-Type", "application/json")
                            .entity("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please try again later.\"}")
                            .build()
            );
        }

        return Uni.createFrom().nullItem();
    }

    @Scheduled(every = "1m")
    void resetCounters() {
        counters.clear();
    }
}
