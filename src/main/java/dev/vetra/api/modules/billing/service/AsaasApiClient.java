package dev.vetra.api.modules.billing.service;

import io.smallrye.mutiny.Uni;
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

@ApplicationScoped
public class AsaasApiClient {

    private static final Logger LOG = Logger.getLogger(AsaasApiClient.class);

    private final Vertx vertx;

    @ConfigProperty(name = "vetra.asaas.api-url", defaultValue = "https://api-sandbox.asaas.com/v3")
    String apiUrl;

    @ConfigProperty(name = "vetra.asaas.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "vetra.asaas.enabled", defaultValue = "false")
    boolean enabled;

    private WebClient webClient;

    @Inject
    public AsaasApiClient(Vertx vertx) {
        this.vertx = vertx;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setConnectTimeout(10000);
        this.webClient = WebClient.create(vertx, options);
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 60000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 2000, jitter = 1000)
    @Timeout(15000)
    public Uni<JsonObject> createCustomer(String name, String cpfCnpj, String email, String externalReference) {
        if (!enabled) {
            LOG.info("Asaas disabled — returning mock customer");
            return Uni.createFrom().item(new JsonObject()
                    .put("id", "cus_mock_" + externalReference)
                    .put("name", name));
        }

        JsonObject body = new JsonObject()
                .put("name", name)
                .put("cpfCnpj", cpfCnpj)
                .put("externalReference", externalReference);
        if (email != null) body.put("email", email);

        LOG.infof("Creating Asaas customer: name=%s", name);
        return post("/customers", body)
                .invoke(result -> LOG.infof("Asaas customer created: %s", result.getString("id")));
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 60000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 2000, jitter = 1000)
    @Timeout(15000)
    public Uni<JsonObject> createPayment(String customerId, String billingType, double value,
                                          String dueDate, String description, String externalReference) {
        if (!enabled) {
            LOG.info("Asaas disabled — returning mock payment");
            return Uni.createFrom().item(new JsonObject()
                    .put("id", "pay_mock_" + externalReference)
                    .put("status", "PENDING")
                    .put("billingType", billingType)
                    .put("value", value));
        }

        JsonObject body = new JsonObject()
                .put("customer", customerId)
                .put("billingType", billingType)
                .put("value", value)
                .put("dueDate", dueDate)
                .put("externalReference", externalReference);
        if (description != null) body.put("description", description);

        LOG.infof("Creating Asaas payment: customer=%s, type=%s, value=%.2f", customerId, billingType, value);
        return post("/payments", body)
                .invoke(result -> LOG.infof("Asaas payment created: %s, status=%s",
                        result.getString("id"), result.getString("status")));
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 60000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 2000, jitter = 1000)
    @Timeout(15000)
    public Uni<JsonObject> getPayment(String paymentId) {
        if (!enabled) {
            return Uni.createFrom().item(new JsonObject().put("id", paymentId).put("status", "PENDING"));
        }
        return get("/payments/" + paymentId);
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 60000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 2000, jitter = 1000)
    @Timeout(15000)
    public Uni<JsonObject> getPixQrCode(String paymentId) {
        if (!enabled) {
            return Uni.createFrom().item(new JsonObject()
                    .put("encodedImage", "mock-qr-base64")
                    .put("payload", "mock-pix-copy-paste"));
        }
        return get("/payments/" + paymentId + "/pixQrCode")
                .onFailure().recoverWithItem(err -> {
                    LOG.warnf("Failed to get PIX QR code for payment %s: %s", paymentId, err.getMessage());
                    return new JsonObject();
                });
    }

    private Uni<JsonObject> post(String path, JsonObject body) {
        String url = apiUrl + path;
        return webClient.postAbs(url)
                .putHeader("access_token", apiKey)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(Buffer.buffer(body.encode()))
                .map(this::handleResponse);
    }

    private Uni<JsonObject> get(String path) {
        String url = apiUrl + path;
        return webClient.getAbs(url)
                .putHeader("access_token", apiKey)
                .send()
                .map(this::handleResponse);
    }

    private JsonObject handleResponse(HttpResponse<Buffer> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            String bodyStr = response.bodyAsString();
            return bodyStr != null && !bodyStr.isBlank() ? new JsonObject(bodyStr) : new JsonObject();
        }
        String errorBody = response.bodyAsString();
        LOG.errorf("Asaas API error: status=%d, body=%s", status, errorBody);
        throw new RuntimeException("Asaas API error: " + status + " - " + errorBody);
    }
}
