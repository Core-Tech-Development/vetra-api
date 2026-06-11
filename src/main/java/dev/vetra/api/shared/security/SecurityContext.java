package dev.vetra.api.shared.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;
import java.util.Set;

/**
 * Helper to extract user information from the Quarkus SecurityIdentity (Keycloak JWT).
 */
@ApplicationScoped
public class SecurityContext {

    private final SecurityIdentity securityIdentity;

    @Inject
    public SecurityContext(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    /**
     * Returns the subject (user id) from the JWT token.
     */
    public Optional<String> userId() {
        if (securityIdentity.isAnonymous()) {
            return Optional.empty();
        }
        return Optional.ofNullable(securityIdentity.getPrincipal().getName());
    }

    /**
     * Returns the set of realm roles assigned to the current user.
     */
    public Set<String> roles() {
        return securityIdentity.getRoles();
    }

    /**
     * Checks whether the current user has the given role.
     */
    public boolean hasRole(String role) {
        return securityIdentity.hasRole(role);
    }

    /**
     * Returns the email claim from the JWT token.
     */
    public Optional<String> email() {
        if (securityIdentity.isAnonymous()) {
            return Optional.empty();
        }
        var principal = securityIdentity.getPrincipal();
        if (principal instanceof JsonWebToken jwt) {
            return Optional.ofNullable(jwt.getClaim("email"));
        }
        return Optional.empty();
    }

    /**
     * Returns true if the current request is authenticated.
     */
    public boolean isAuthenticated() {
        return !securityIdentity.isAnonymous();
    }
}
