package dev.vetra.api.modules.billing.resource;

import dev.vetra.api.modules.billing.usecase.ProcessAsaasWebhookUseCase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@Path("/api/v1/webhooks/asaas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Webhooks", description = "Asaas payment webhook receiver")
public class AsaasWebhookResource {

    private static final Logger LOG = Logger.getLogger(AsaasWebhookResource.class);

    private final ProcessAsaasWebhookUseCase processWebhookUseCase;

    @ConfigProperty(name = "vetra.asaas.webhook-token", defaultValue = "")
    String webhookToken;

    @Inject
    public AsaasWebhookResource(ProcessAsaasWebhookUseCase processWebhookUseCase) {
        this.processWebhookUseCase = processWebhookUseCase;
    }

    @POST
    @Operation(summary = "Receive Asaas webhook events")
    public Uni<Response> receiveWebhook(@HeaderParam("asaas-access-token") String accessToken,
                                         String rawPayload) {
        if (webhookToken != null && !webhookToken.isBlank() && !webhookToken.equals(accessToken)) {
            LOG.warnf("Invalid webhook token received");
            return Uni.createFrom().item(Response.status(401).build());
        }

        try {
            JsonObject payload = new JsonObject(rawPayload);
            String event = payload.getString("event");
            JsonObject payment = payload.getJsonObject("payment");
            String paymentId = payment != null ? payment.getString("id") : null;

            return processWebhookUseCase.execute(null, event, paymentId, rawPayload)
                    .map(v -> Response.ok().build())
                    .onFailure().recoverWithItem(err -> {
                        LOG.errorf(err, "Error processing webhook");
                        return Response.ok().build();
                    });
        } catch (Exception e) {
            LOG.errorf(e, "Error parsing webhook payload");
            return Uni.createFrom().item(Response.ok().build());
        }
    }
}
