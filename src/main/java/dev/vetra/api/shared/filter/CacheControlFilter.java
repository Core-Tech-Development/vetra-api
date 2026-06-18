package dev.vetra.api.shared.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * JAX-RS response filter that adds Cache-Control headers to API responses.
 *
 * <p>Rules:
 * <ul>
 *   <li>Non-GET methods (POST, PUT, PATCH, DELETE) always receive {@code no-store}.</li>
 *   <li>Quarkus internal endpoints ({@code /q/*}) are skipped entirely.</li>
 *   <li>Auth endpoints ({@code /api/v1/auth/*}) receive {@code no-store}.</li>
 *   <li>Public cacheable endpoints (pricing, available-slots) receive {@code public, max-age=300}.</li>
 *   <li>All other authenticated GET endpoints receive {@code private, no-cache}.</li>
 * </ul>
 */
@Provider
public class CacheControlFilter implements ContainerResponseFilter {

    private static final String CACHE_CONTROL = "Cache-Control";

    private static final String NO_STORE = "no-store";
    private static final String PUBLIC_5_MIN = "public, max-age=300";
    private static final String PRIVATE_REVALIDATE = "private, no-cache";

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        // If a Cache-Control header was already set explicitly by a resource method, respect it
        if (responseContext.getHeaders().containsKey(CACHE_CONTROL)) {
            return;
        }

        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        // Non-GET mutations must never be cached
        if (!"GET".equals(method)) {
            responseContext.getHeaders().putSingle(CACHE_CONTROL, NO_STORE);
            return;
        }

        // Skip Quarkus internal endpoints (health, metrics, openapi, swagger-ui)
        if (path.startsWith("q/") || path.startsWith("/q/")) {
            return;
        }

        // Auth endpoints - sensitive, never cache
        if (path.startsWith("api/v1/auth") || path.startsWith("/api/v1/auth")) {
            responseContext.getHeaders().putSingle(CACHE_CONTROL, NO_STORE);
            return;
        }

        // Public cacheable data - pricing and available-slots can be cached for 5 minutes
        if (containsPublicCacheablePath(path)) {
            responseContext.getHeaders().putSingle(CACHE_CONTROL, PUBLIC_5_MIN);
            return;
        }

        // All other authenticated GET endpoints - browser may store but must revalidate
        responseContext.getHeaders().putSingle(CACHE_CONTROL, PRIVATE_REVALIDATE);
    }

    /**
     * Checks whether the given path corresponds to a public, read-only endpoint
     * whose response is safe to cache in shared caches.
     */
    private boolean containsPublicCacheablePath(String path) {
        return path.contains("/pricing") || path.contains("/available-slots");
    }
}
