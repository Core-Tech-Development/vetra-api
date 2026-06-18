package dev.vetra.api.shared.security;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.vetra.api.shared.cache.CacheKeys;
import dev.vetra.api.shared.cache.CacheTtl;
import dev.vetra.api.shared.cache.ReactiveCacheService;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Service to manage users in Keycloak via the Admin REST API.
 * Used during registration to create user accounts.
 */
@ApplicationScoped
public class KeycloakAdminService {

    private static final Logger LOG = Logger.getLogger(KeycloakAdminService.class);

    private static final TypeReference<String> STRING_REF = new TypeReference<>() {};

    private final Vertx vertx;
    private final ReactiveCacheService cache;
    private final CacheTtl ttl;

    @ConfigProperty(name = "vetra.keycloak.admin-url", defaultValue = "http://keycloak:8080")
    String keycloakUrl;

    @ConfigProperty(name = "vetra.keycloak.realm", defaultValue = "vetra")
    String realm;

    @ConfigProperty(name = "vetra.keycloak.admin-username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "vetra.keycloak.admin-password", defaultValue = "admin")
    String adminPassword;

    private WebClient webClient;

    @Inject
    public KeycloakAdminService(Vertx vertx, ReactiveCacheService cache, CacheTtl ttl) {
        this.vertx = vertx;
        this.cache = cache;
        this.ttl = ttl;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setTrustAll(true)
                .setConnectTimeout(5000);
        this.webClient = WebClient.create(vertx, options);
    }

    /**
     * Creates a user in Keycloak with the given credentials and realm roles.
     */
    public Uni<String> createUser(String email, String password, String firstName, List<String> realmRoles) {
        return getAdminToken()
                .flatMap(token -> createKeycloakUser(token, email, firstName))
                .flatMap(result -> {
                    String token = result.getString("token");
                    String userId = result.getString("userId");
                    return setUserPassword(token, userId, password)
                            .flatMap(v -> assignRealmRoles(token, userId, realmRoles))
                            .map(v -> userId);
                });
    }

    private Uni<String> getAdminToken() {
        return cache.getOrLoad(
                CacheKeys.keycloakAdminToken(),
                ttl.keycloakToken(),
                STRING_REF,
                fetchAdminTokenFromKeycloak());
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 500, jitter = 200)
    @Timeout(5000)
    Uni<String> fetchAdminTokenFromKeycloak() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        String body = "client_id=admin-cli"
                + "&username=" + adminUsername
                + "&password=" + adminPassword
                + "&grant_type=password";

        return webClient.postAbs(tokenUrl)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendBuffer(Buffer.buffer(body))
                .map(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Failed to get Keycloak admin token: " + response.statusCode());
                    }
                    return response.bodyAsJsonObject().getString("access_token");
                });
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 1, delay = 1000)
    @Timeout(10000)
    Uni<JsonObject> createKeycloakUser(String token, String email, String fullName) {
        String usersUrl = keycloakUrl + "/admin/realms/" + realm + "/users";

        String firstName;
        String lastName;
        if (fullName != null && fullName.contains(" ")) {
            int spaceIdx = fullName.indexOf(' ');
            firstName = fullName.substring(0, spaceIdx);
            lastName = fullName.substring(spaceIdx + 1);
        } else {
            firstName = fullName != null ? fullName : email;
            lastName = "-";
        }

        JsonObject userPayload = new JsonObject()
                .put("username", email)
                .put("email", email)
                .put("firstName", firstName)
                .put("lastName", lastName)
                .put("enabled", true)
                .put("emailVerified", true);

        return webClient.postAbs(usersUrl)
                .putHeader("Authorization", "Bearer " + token)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(userPayload)
                .flatMap(response -> {
                    if (response.statusCode() == 201) {
                        String location = response.getHeader("Location");
                        String userId = location.substring(location.lastIndexOf('/') + 1);
                        LOG.infof("Keycloak user created: userId=%s, email=%s", userId, email);
                        return Uni.createFrom().item(new JsonObject()
                                .put("token", token)
                                .put("userId", userId));
                    } else if (response.statusCode() == 409) {
                        LOG.warnf("Keycloak user already exists: email=%s", email);
                        return findUserByEmail(token, email)
                                .map(userId -> new JsonObject()
                                        .put("token", token)
                                        .put("userId", userId));
                    } else {
                        String errorBody = response.bodyAsString();
                        LOG.errorf("Failed to create Keycloak user: status=%d, body=%s", response.statusCode(), errorBody);
                        return Uni.createFrom().failure(
                                new RuntimeException("Failed to create Keycloak user: " + response.statusCode()));
                    }
                });
    }

    private Uni<String> findUserByEmail(String token, String email) {
        String searchUrl = keycloakUrl + "/admin/realms/" + realm + "/users?email=" + email + "&exact=true";

        return webClient.getAbs(searchUrl)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    JsonArray users = response.bodyAsJsonArray();
                    if (users.isEmpty()) {
                        throw new RuntimeException("User not found by email: " + email);
                    }
                    return users.getJsonObject(0).getString("id");
                });
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 1, delay = 1000)
    @Timeout(10000)
    Uni<Void> setUserPassword(String token, String userId, String password) {
        String credUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        JsonObject credential = new JsonObject()
                .put("type", "password")
                .put("value", password)
                .put("temporary", false);

        return webClient.putAbs(credUrl)
                .putHeader("Authorization", "Bearer " + token)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(credential)
                .map(response -> {
                    if (response.statusCode() != 204) {
                        LOG.errorf("Failed to set password for user %s: status=%d", userId, response.statusCode());
                        throw new RuntimeException("Failed to set user password: " + response.statusCode());
                    }
                    LOG.infof("Password set for Keycloak user: userId=%s", userId);
                    return null;
                });
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 1, delay = 1000)
    @Timeout(10000)
    Uni<Void> assignRealmRoles(String token, String userId, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        return getRoleRepresentations(token, roleNames)
                .flatMap(roles -> {
                    String roleMappingUrl = keycloakUrl + "/admin/realms/" + realm
                            + "/users/" + userId + "/role-mappings/realm";

                    return webClient.postAbs(roleMappingUrl)
                            .putHeader("Authorization", "Bearer " + token)
                            .putHeader("Content-Type", "application/json")
                            .sendBuffer(Buffer.buffer(roles.encode()))
                            .map(response -> {
                                if (response.statusCode() != 204) {
                                    LOG.errorf("Failed to assign roles to user %s: status=%d", userId, response.statusCode());
                                    throw new RuntimeException("Failed to assign roles: " + response.statusCode());
                                }
                                LOG.infof("Roles %s assigned to Keycloak user: userId=%s", roleNames, userId);
                                return null;
                            });
                });
    }

    private Uni<JsonArray> getRoleRepresentations(String token, List<String> roleNames) {
        String rolesUrl = keycloakUrl + "/admin/realms/" + realm + "/roles";

        return webClient.getAbs(rolesUrl)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    JsonArray allRoles = response.bodyAsJsonArray();
                    JsonArray matched = new JsonArray();
                    for (int i = 0; i < allRoles.size(); i++) {
                        JsonObject role = allRoles.getJsonObject(i);
                        if (roleNames.contains(role.getString("name"))) {
                            matched.add(new JsonObject()
                                    .put("id", role.getString("id"))
                                    .put("name", role.getString("name")));
                        }
                    }
                    return matched;
                });
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 1, delay = 1000)
    @Timeout(10000)
    public Uni<Void> disableUser(String userId) {
        return getAdminToken()
                .flatMap(token -> {
                    String userUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId;
                    JsonObject payload = new JsonObject().put("enabled", false);
                    return webClient.putAbs(userUrl)
                            .putHeader("Authorization", "Bearer " + token)
                            .putHeader("Content-Type", "application/json")
                            .sendJsonObject(payload)
                            .map(response -> {
                                if (response.statusCode() != 204) {
                                    LOG.warnf("Failed to disable Keycloak user %s: status=%d", userId, response.statusCode());
                                } else {
                                    LOG.infof("Keycloak user disabled: userId=%s", userId);
                                }
                                return (Void) null;
                            });
                });
    }
}
