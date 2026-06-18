package dev.vetra.api.modules.identity.usecase;

import dev.vetra.api.modules.identity.dto.ChangePasswordRequest;
import dev.vetra.api.shared.exception.BusinessException;
import dev.vetra.api.shared.security.SecurityContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Changes the password for the currently authenticated user.
 *
 * Steps:
 * 1. Extract userId and email from SecurityContext (JWT).
 * 2. Verify the current password by attempting a token grant against Keycloak.
 * 3. Set the new password via the Keycloak Admin REST API.
 */
@ApplicationScoped
public class ChangePasswordUseCase {

    private static final Logger LOG = Logger.getLogger(ChangePasswordUseCase.class);

    private final SecurityContext securityContext;
    private final Vertx vertx;

    @ConfigProperty(name = "vetra.keycloak.admin-url", defaultValue = "http://keycloak:8080")
    String keycloakUrl;

    @ConfigProperty(name = "vetra.keycloak.realm", defaultValue = "vetra")
    String realm;

    @ConfigProperty(name = "vetra.keycloak.web-client-id", defaultValue = "vetra-web")
    String webClientId;

    @ConfigProperty(name = "vetra.keycloak.admin-username", defaultValue = "admin")
    String adminUsername;

    @ConfigProperty(name = "vetra.keycloak.admin-password", defaultValue = "admin")
    String adminPassword;

    private WebClient webClient;

    @Inject
    public ChangePasswordUseCase(SecurityContext securityContext, Vertx vertx) {
        this.securityContext = securityContext;
        this.vertx = vertx;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setTrustAll(true)
                .setConnectTimeout(5000);
        this.webClient = WebClient.create(vertx, options);
    }

    public Uni<Void> execute(ChangePasswordRequest request) {
        String userId = securityContext.userId()
                .orElseThrow(() -> new BusinessException("UNAUTHENTICATED", "User is not authenticated"));
        String email = securityContext.email()
                .orElseThrow(() -> new BusinessException("UNAUTHENTICATED", "User email not available in token"));

        return verifyCurrentPassword(email, request.currentPassword())
                .flatMap(valid -> getAdminToken())
                .flatMap(adminToken -> setUserPassword(adminToken, userId, request.newPassword()))
                .invoke(() -> LOG.infof("Password changed successfully for userId=%s", userId));
    }

    /**
     * Verifies the current password by attempting a Resource Owner Password Credentials
     * grant against the Keycloak token endpoint. If Keycloak returns a non-200 status,
     * the current password is invalid.
     */
    @Timeout(8000)
    Uni<Void> verifyCurrentPassword(String email, String currentPassword) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String body = "grant_type=password"
                + "&client_id=" + URLEncoder.encode(webClientId, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(currentPassword, StandardCharsets.UTF_8);

        return webClient.postAbs(tokenUrl)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendBuffer(Buffer.buffer(body))
                .map(response -> {
                    if (response.statusCode() != 200) {
                        LOG.warnf("Current password verification failed for email=%s, status=%d",
                                email, response.statusCode());
                        throw new BusinessException("INVALID_CURRENT_PASSWORD",
                                "The current password is incorrect");
                    }
                    return (Void) null;
                });
    }

    /**
     * Obtains an admin access token from the Keycloak master realm.
     */
    private Uni<String> getAdminToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        String body = "client_id=admin-cli"
                + "&username=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8)
                + "&grant_type=password";

        return webClient.postAbs(tokenUrl)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendBuffer(Buffer.buffer(body))
                .map(response -> {
                    if (response.statusCode() != 200) {
                        LOG.errorf("Failed to get Keycloak admin token: status=%d", response.statusCode());
                        throw new RuntimeException(
                                "Failed to get Keycloak admin token: " + response.statusCode());
                    }
                    return response.bodyAsJsonObject().getString("access_token");
                });
    }

    /**
     * Sets the new password for the user via the Keycloak Admin REST API.
     */
    private Uni<Void> setUserPassword(String adminToken, String userId, String newPassword) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        JsonObject credential = new JsonObject()
                .put("type", "password")
                .put("value", newPassword)
                .put("temporary", false);

        return webClient.putAbs(url)
                .putHeader("Authorization", "Bearer " + adminToken)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(credential)
                .map(response -> {
                    if (response.statusCode() != 204) {
                        LOG.errorf("Failed to set new password for userId=%s: status=%d, body=%s",
                                userId, response.statusCode(), response.bodyAsString());
                        throw new RuntimeException(
                                "Failed to set new password: " + response.statusCode());
                    }
                    LOG.infof("New password set via Keycloak Admin API for userId=%s", userId);
                    return (Void) null;
                });
    }
}
