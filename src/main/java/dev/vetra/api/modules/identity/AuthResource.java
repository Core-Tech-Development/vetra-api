package dev.vetra.api.modules.identity;

import dev.vetra.api.modules.identity.dto.ChangePasswordRequest;
import dev.vetra.api.modules.identity.usecase.ChangePasswordUseCase;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
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
@Tag(name = "Authentication", description = "Login, CAPTCHA, and password reset endpoints")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    private final CaptchaService captchaService;
    private final SesEmailService sesEmailService;
    private final PasswordResetTokenService tokenService;
    private final ChangePasswordUseCase changePasswordUseCase;
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

    @ConfigProperty(name = "vetra.frontend.url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    private WebClient webClient;

    @Inject
    public AuthResource(CaptchaService captchaService, SesEmailService sesEmailService,
                        PasswordResetTokenService tokenService, ChangePasswordUseCase changePasswordUseCase,
                        Vertx vertx) {
        this.captchaService = captchaService;
        this.sesEmailService = sesEmailService;
        this.tokenService = tokenService;
        this.changePasswordUseCase = changePasswordUseCase;
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
        if (!captchaService.verifySolution(request.captcha())) {
            LOG.warnf("CAPTCHA verification failed for user: %s", request.username());
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("captcha_failed",
                                    "CAPTCHA verification failed. Please try again."))
                            .build());
        }

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

                    String error = responseBody != null ? responseBody.getString("error", "") : "";
                    String errorDescription = responseBody != null
                            ? responseBody.getString("error_description", "") : "";

                    LOG.warnf("Login failed for user: %s, error: %s, description: %s",
                            request.username(), error, errorDescription);

                    if ("invalid_grant".equals(error)) {
                        if (errorDescription != null && (
                                errorDescription.toLowerCase().contains("temporarily disabled")
                                        || errorDescription.toLowerCase().contains("temporarily locked"))) {
                            return Response.status(Response.Status.UNAUTHORIZED)
                                    .entity(new ErrorResponse("user_temporarily_disabled",
                                            "Account temporarily locked due to too many failed attempts. Try again in 30 minutes."))
                                    .build();
                        }
                        if (errorDescription != null && (
                                errorDescription.toLowerCase().contains("disabled")
                                        || errorDescription.toLowerCase().contains("locked"))) {
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
                            .entity(new ErrorResponse(error,
                                    errorDescription.isEmpty() ? "Authentication failed." : errorDescription))
                            .build();
                });
    }

    // ── Forgot Password ─────────────────────────────────────────────────────────

    @POST
    @Path("/forgot-password")
    @Operation(summary = "Request password reset",
            description = "Sends a password reset email via AWS SES")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "Reset email sent (or user not found — same response for security)"),
            @APIResponse(responseCode = "400", description = "Invalid email format")
    })
    public Uni<Response> forgotPassword(@Valid ForgotPasswordRequest request) {
        Response successResponse = Response.ok(
                new MessageResponse("If an account with that email exists, a password reset link has been sent.")
        ).build();

        return getAdminToken()
                .flatMap(token -> findUserByEmail(token, request.email()))
                .flatMap(result -> {
                    if (result == null) {
                        LOG.infof("Password reset requested for non-existent email: %s",
                                request.email());
                        return Uni.createFrom().item(successResponse);
                    }

                    String userId = result.getString("userId");
                    String resetToken = tokenService.generateToken(userId);
                    String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

                    String htmlBody = buildResetEmailHtml(resetUrl);
                    String textBody = "Reset your Vetra password by visiting: " + resetUrl
                            + "\n\nThis link expires in 1 hour."
                            + "\n\nIf you did not request this, ignore this email.";

                    return sesEmailService.sendEmail(
                                    request.email(),
                                    "Reset your Vetra password",
                                    htmlBody,
                                    textBody)
                            .map(v -> successResponse);
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf("Failed to process password reset for %s: %s",
                            request.email(), err.getMessage());
                    return successResponse;
                });
    }

    // ── Reset Password ──────────────────────────────────────────────────────────

    @POST
    @Path("/reset-password")
    @Operation(summary = "Reset password with token",
            description = "Validates the reset token and sets a new password")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Password reset successful"),
            @APIResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public Uni<Response> resetPassword(@Valid ResetPasswordRequest request) {
        String userId = tokenService.validateAndGetUserId(request.token());
        if (userId == null) {
            return Uni.createFrom().item(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("invalid_token",
                                    "The reset link is invalid or has expired. Please request a new one."))
                            .build());
        }

        return getAdminToken()
                .flatMap(adminToken -> resetKeycloakPassword(adminToken, userId, request.newPassword()))
                .map(v -> Response.ok(
                        new MessageResponse("Password has been reset successfully.")).build())
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf("Failed to reset password for userId %s: %s", userId, err.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse("reset_failed",
                                    "Failed to reset password. Please try again."))
                            .build();
                });
    }

    // ── Change Password (authenticated) ────────────────────────────────────────

    @POST
    @Path("/change-password")
    @Authenticated
    @Operation(summary = "Change password for authenticated user")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Password changed successfully"),
            @APIResponse(responseCode = "400", description = "Invalid current password or validation error"),
            @APIResponse(responseCode = "401", description = "Not authenticated")
    })
    public Uni<Response> changePassword(@Valid ChangePasswordRequest request) {
        return changePasswordUseCase.execute(request)
                .replaceWith(Response.noContent().build());
    }

    // ── Keycloak Admin helpers ──────────────────────────────────────────────────

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
                        throw new RuntimeException(
                                "Failed to get Keycloak admin token: " + response.statusCode());
                    }
                    return response.bodyAsJsonObject().getString("access_token");
                });
    }

    private Uni<JsonObject> findUserByEmail(String token, String email) {
        String searchUrl = keycloakUrl + "/admin/realms/" + realm + "/users?email="
                + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&exact=true";

        return webClient.getAbs(searchUrl)
                .putHeader("Authorization", "Bearer " + token)
                .send()
                .map(response -> {
                    if (response.statusCode() != 200) return null;
                    JsonArray users = response.bodyAsJsonArray();
                    if (users == null || users.isEmpty()) return null;
                    return new JsonObject()
                            .put("userId", users.getJsonObject(0).getString("id"));
                });
    }

    private Uni<Void> resetKeycloakPassword(String adminToken, String userId, String newPassword) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        JsonObject credential = new JsonObject()
                .put("type", "password")
                .put("value", newPassword)
                .put("temporary", false);

        return webClient.putAbs(url)
                .putHeader("Authorization", "Bearer " + adminToken)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(credential.encode()))
                .map(response -> {
                    if (response.statusCode() != 204 && response.statusCode() != 200) {
                        throw new RuntimeException("Keycloak reset-password failed: status="
                                + response.statusCode() + ", body=" + response.bodyAsString());
                    }
                    LOG.infof("Password reset via Keycloak for userId: %s", userId);
                    return null;
                });
    }

    // ── Email template ──────────────────────────────────────────────────────────

    private String buildResetEmailHtml(String resetUrl) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background-color:#F7FAF8;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 20px;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0" style="background:#fff;border-radius:12px;border:1px solid #D7E3DC;padding:40px;">
                        <tr><td style="text-align:center;padding-bottom:24px;">
                          <span style="font-size:24px;font-weight:700;color:#1F6F5B;">Vetra</span>
                        </td></tr>
                        <tr><td style="font-size:20px;font-weight:600;color:#17211B;padding-bottom:16px;">
                          Redefinir sua senha
                        </td></tr>
                        <tr><td style="font-size:14px;color:#4F6257;line-height:1.6;padding-bottom:24px;">
                          Recebemos uma solicitacao para redefinir a senha da sua conta Vetra.
                          Clique no botao abaixo para criar uma nova senha. Este link expira em 1 hora.
                        </td></tr>
                        <tr><td style="padding-bottom:24px;">
                          <a href="%s"
                             style="display:inline-block;padding:12px 32px;background-color:#1F6F5B;color:#fff;font-size:14px;font-weight:600;text-decoration:none;border-radius:8px;">
                            Redefinir senha
                          </a>
                        </td></tr>
                        <tr><td style="font-size:12px;color:#4F6257;line-height:1.5;padding-bottom:16px;">
                          Se voce nao solicitou a redefinicao de senha, ignore este email.
                          Sua senha permanecera inalterada.
                        </td></tr>
                        <tr><td style="border-top:1px solid #D7E3DC;padding-top:16px;font-size:11px;color:#9BA8A0;">
                          Vetra — Plataforma de Diagnostico Veterinario por Imagem
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(resetUrl);
    }

    @RegisterForReflection
    public record ErrorResponse(String error, String message) {}

    @RegisterForReflection
    public record MessageResponse(String message) {}
}
