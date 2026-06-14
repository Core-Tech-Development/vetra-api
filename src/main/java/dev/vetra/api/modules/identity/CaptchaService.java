package dev.vetra.api.modules.identity;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Altcha-compatible proof-of-work CAPTCHA service.
 * Generates challenges and verifies solutions without any external service.
 */
@ApplicationScoped
public class CaptchaService {

    private static final Logger LOG = Logger.getLogger(CaptchaService.class);
    private static final String ALGORITHM = "SHA-256";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MAX_NUMBER = 100_000;
    private static final long CHALLENGE_TTL_MS = 300_000; // 5 minutes

    @ConfigProperty(name = "vetra.captcha.hmac-key", defaultValue = "vetra-captcha-default-key-change-me")
    String hmacKey;

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Long> usedChallenges = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        LOG.info("CaptchaService initialized");
    }

    /**
     * Generates a new Altcha challenge.
     */
    public CaptchaChallenge createChallenge() {
        int secretNumber = random.nextInt(MAX_NUMBER) + 1;
        long expires = System.currentTimeMillis() + CHALLENGE_TTL_MS;
        String salt = generateSalt() + "?expires=" + expires;

        String challenge = sha256Hex(salt + secretNumber);
        String signature = hmacSha256Hex(challenge);

        return new CaptchaChallenge(ALGORITHM, challenge, MAX_NUMBER, salt, signature);
    }

    /**
     * Verifies an Altcha solution payload (Base64-encoded JSON).
     */
    public boolean verifySolution(String base64Payload) {
        if (base64Payload == null || base64Payload.isBlank()) {
            return false;
        }

        try {
            String json = new String(Base64.getDecoder().decode(base64Payload), StandardCharsets.UTF_8);
            Map<String, Object> payload = parseSimpleJson(json);

            String algorithm = (String) payload.get("algorithm");
            String challenge = (String) payload.get("challenge");
            String salt = (String) payload.get("salt");
            String signature = (String) payload.get("signature");
            int number = ((Number) payload.get("number")).intValue();

            if (!ALGORITHM.equals(algorithm)) {
                LOG.warn("CAPTCHA verification failed: invalid algorithm");
                return false;
            }

            // Check expiration
            if (salt != null && salt.contains("?expires=")) {
                String expiresStr = salt.substring(salt.indexOf("?expires=") + 9);
                long expires = Long.parseLong(expiresStr);
                if (System.currentTimeMillis() > expires) {
                    LOG.warn("CAPTCHA verification failed: challenge expired");
                    return false;
                }
            }

            // Replay protection
            if (usedChallenges.putIfAbsent(challenge, System.currentTimeMillis()) != null) {
                LOG.warn("CAPTCHA verification failed: challenge already used");
                return false;
            }

            // Verify hash: SHA-256(salt + number) == challenge
            String expectedChallenge = sha256Hex(salt + number);
            if (!expectedChallenge.equals(challenge)) {
                LOG.warn("CAPTCHA verification failed: hash mismatch");
                return false;
            }

            // Verify signature: HMAC-SHA-256(challenge, key) == signature
            String expectedSignature = hmacSha256Hex(challenge);
            if (!expectedSignature.equals(signature)) {
                LOG.warn("CAPTCHA verification failed: signature mismatch");
                return false;
            }

            return true;
        } catch (Exception e) {
            LOG.warnf("CAPTCHA verification error: %s", e.getMessage());
            return false;
        } finally {
            cleanupExpiredChallenges();
        }
    }

    private String generateSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String hmacSha256Hex(String input) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(hmacKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA-256 failed", e);
        }
    }

    private void cleanupExpiredChallenges() {
        long cutoff = System.currentTimeMillis() - CHALLENGE_TTL_MS;
        usedChallenges.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    /**
     * Minimal JSON parser for the Altcha payload.
     * Avoids adding a JSON library dependency for this simple structure.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSimpleJson(String json) {
        // Use Jackson via Vert.x JsonObject for parsing
        io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(json);
        return obj.getMap();
    }

    public record CaptchaChallenge(
            String algorithm,
            String challenge,
            int maxnumber,
            String salt,
            String signature
    ) {}
}
