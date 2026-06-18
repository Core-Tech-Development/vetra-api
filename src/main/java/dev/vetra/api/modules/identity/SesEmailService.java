package dev.vetra.api.modules.identity;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Map;

/**
 * Sends emails via AWS SES v2 HTTPS API (port 443).
 * Uses AWS Signature V4 for authentication — no AWS SDK dependency needed.
 */
@ApplicationScoped
public class SesEmailService {

    private static final Logger LOG = Logger.getLogger(SesEmailService.class);

    @ConfigProperty(name = "vetra.aws.access-key-id", defaultValue = "")
    String accessKeyId;

    @ConfigProperty(name = "vetra.aws.secret-access-key", defaultValue = "")
    String secretAccessKey;

    @ConfigProperty(name = "vetra.aws.region", defaultValue = "us-east-2")
    String region;

    @ConfigProperty(name = "vetra.email.from", defaultValue = "noreply@vetra.vet.br")
    String fromAddress;

    @ConfigProperty(name = "vetra.email.enabled", defaultValue = "false")
    boolean emailEnabled;

    private final Vertx vertx;
    private WebClient webClient;

    @Inject
    public SesEmailService(Vertx vertx) {
        this.vertx = vertx;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setConnectTimeout(10000);
        this.webClient = WebClient.create(vertx, options);
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Retry(maxRetries = 2, delay = 1000)
    @Timeout(10000)
    @Fallback(fallbackMethod = "sendEmailFallback")
    public Uni<Void> sendEmail(String to, String subject, String htmlBody, String textBody) {
        if (!emailEnabled || accessKeyId.isEmpty() || secretAccessKey.isEmpty()) {
            LOG.warnf("Email not configured. Would have sent to: %s, subject: %s", to, subject);
            return Uni.createFrom().voidItem();
        }

        String host = "email." + region + ".amazonaws.com";
        String path = "/v2/email/outbound-emails";
        URI uri = URI.create("https://" + host + path);

        JsonObject requestBody = new JsonObject()
                .put("Content", new JsonObject()
                        .put("Simple", new JsonObject()
                                .put("Subject", new JsonObject()
                                        .put("Data", subject)
                                        .put("Charset", "UTF-8"))
                                .put("Body", new JsonObject()
                                        .put("Html", new JsonObject()
                                                .put("Data", htmlBody)
                                                .put("Charset", "UTF-8"))
                                        .put("Text", new JsonObject()
                                                .put("Data", textBody)
                                                .put("Charset", "UTF-8")))))
                .put("Destination", new JsonObject()
                        .put("ToAddresses", new JsonArray().add(to)))
                .put("FromEmailAddress", fromAddress);

        String body = requestBody.encode();

        Map<String, String> signedHeaders = AwsSigV4Signer.sign(
                "POST", uri, "application/json", body,
                accessKeyId, secretAccessKey, region, "ses");

        var request = webClient.postAbs(uri.toString());
        for (var entry : signedHeaders.entrySet()) {
            request.putHeader(entry.getKey(), entry.getValue());
        }

        return request.sendBuffer(Buffer.buffer(body))
                .map(response -> {
                    if (response.statusCode() == 200) {
                        LOG.infof("Email sent successfully to %s", to);
                    } else {
                        LOG.errorf("Failed to send email to %s: status=%d, body=%s",
                                to, response.statusCode(), response.bodyAsString());
                    }
                    return null;
                });
    }

    /**
     * Fallback when email delivery fails (circuit open, timeout, or retries exhausted).
     * Logs the failure but does not throw, so the calling use case can continue.
     */
    Uni<Void> sendEmailFallback(String to, String subject, String htmlBody, String textBody) {
        LOG.warnf("Email delivery failed (circuit open or timeout), skipping email to %s: %s", to, subject);
        return Uni.createFrom().voidItem();
    }
}
