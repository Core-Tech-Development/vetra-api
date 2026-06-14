package dev.vetra.api.modules.identity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Public authentication endpoints with CAPTCHA protection.
 * Proxies login requests to Keycloak after verifying the CAPTCHA solution.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Login and CAPTCHA endpoints")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    private final CaptchaService captchaService;
    private final Vertx vertx;

    @ConfigProperty(name = "vetra.keycloak.admin-url", defaultValue = "http://keycloak:8080")
    String keycloakUrl;

    @ConfigProperty(name = "vetra.keycloak.realm", defaultValue = "vetra")
    String realm;

    @ConfigProperty(name = "vetra.keycloak.web-client-id", defaultValue = "vetra-web")
    String webClientId;

    private WebClient webClient;

    @Inject
    public AuthResource(CaptchaService captchaService, Vertx vertx) {
        this.captchaService = captchaService;
        this.vertx = vertx;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setTrustAll(true)
                .setConnectTimeout(5000);
        this.webClient = WebClient.create(vertx, options);
    }

    @GET
    @Path("/captcha-challenge")
    @Operation(summary = "Get CAPTCHA challenge", description = "Returns an Altcha-compatible proof-of-work challenge")
    @APIResponse(responseCode = "200", description = "Challenge generated")
    public Response getCaptchaChallenge() {
        CaptchaService.CaptchaChallenge challenge = captchaService.createChallenge();
        return Response.ok(challenge).build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Login with CAPTCHA", description = "Verifies CAPTCHA then authenticates with Keycloak")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Authentication successful"),
            @APIResponse(responseCode = "400", description = "Invalid CAPTCHA or missing fields"),
            @APIResponse(responseCode = "401", description = "Invalid credentials or account locked")
    })
    public Uni<Response> login(@Valid LoginRequest request) {
        // Step 1: Verify CAPTCHA
        if (!captchaService.verifySolution(request.captcha())) {
            LOG.warnf("CAPTCHA verification failed for user: %s", request.username());
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("captcha_failed", "CAPTCHA verification failed. Please try again."))
                            .build()
            );
        }

        // Step 2: Forward to Keycloak token endpoint
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String body = "grant_type=password"
                + "&client_id=" + URLEncoder.encode(webClientId, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(request.username(), StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(request.password(), StandardCharsets.UTF_8)
                + "&scope=openid";

        return webClient.postAbs(tokenUrl)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendBuffer(Buffer.buffer(body))
                .map(keycloakResponse -> {
                    int status = keycloakResponse.statusCode();
                    JsonObject responseBody = keycloakResponse.bodyAsJsonObject();

                    if (status == 200) {
                        LOG.infof("Login successful for user: %s", request.username());
                        return Response.ok(responseBody.encode())
                                .type(MediaType.APPLICATION_JSON)
                                .build();
                    }

                    // Handle Keycloak error responses
                    String error = responseBody != null ? responseBody.getString("error", "") : "";
                    String errorDescription = responseBody != null ? responseBody.getString("error_description", "") : "";

                    LOG.warnf("Login failed for user: %s, error: %s, description: %s",
                            request.username(), error, errorDescription);

                    if ("invalid_grant".equals(error)) {
                        // Check if user is temporarily locked
                        if (errorDescription != null && (
                                errorDescription.toLowerCase().contains("temporarily disabled") ||
                                errorDescription.toLowerCase().contains("temporarily locked"))) {
                            return Response.status(Response.Status.UNAUTHORIZED)
                                    .entity(new ErrorResponse("user_temporarily_disabled",
                                            "Account temporarily locked due to too many failed attempts. Try again in 30 minutes."))
                                    .build();
                        }
                        // Check if user is permanently disabled
                        if (errorDescription != null && (
                                errorDescription.toLowerCase().contains("disabled") ||
                                errorDescription.toLowerCase().contains("locked"))) {
                            return Response.status(Response.Status.UNAUTHORIZED)
                                    .entity(new ErrorResponse("user_disabled",
                                            "Account is disabled. Contact your administrator."))
                                    .build();
                        }
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity(new ErrorResponse("invalid_grant", "Invalid username or password."))
                                .build();
                    }

                    if ("unauthorized_client".equals(error)) {
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity(new ErrorResponse("unauthorized_client",
                                        "Login method is not enabled. Contact your administrator."))
                                .build();
                    }

                    return Response.status(Response.Status.UNAUTHORIZED)
                            .entity(new ErrorResponse(error, errorDescription.isEmpty() ? "Authentication failed." : errorDescription))
                            .build();
                });
    }

    @RegisterForReflection
    public record ErrorResponse(String error, String message) {}
}
