package dev.vetra.api.modules.identity;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Generates and validates self-contained password reset tokens using HMAC.
 * Tokens are stateless — no server-side storage needed.
 * Format: base64url(userId:expiryEpochSeconds:hmacSignature)
 */
@ApplicationScoped
public class PasswordResetTokenService {

    private static final long TOKEN_VALIDITY_SECONDS = 3600; // 1 hour

    @ConfigProperty(name = "vetra.captcha.hmac-key")
    String hmacKey;

    public String generateToken(String userId) {
        long expiry = Instant.now().getEpochSecond() + TOKEN_VALIDITY_SECONDS;
        String data = userId + ":" + expiry;
        String signature = computeHmac(data);
        String token = data + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates the token and returns the userId if valid, or null if invalid/expired.
     */
    public String validateAndGetUserId(String token) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 3);
            if (parts.length != 3) return null;

            String userId = parts[0];
            long expiry = Long.parseLong(parts[1]);
            String signature = parts[2];

            if (Instant.now().getEpochSecond() > expiry) return null;

            String expectedSignature = computeHmac(userId + ":" + expiry);
            if (!signature.equals(expectedSignature)) return null;

            return userId;
        } catch (Exception e) {
            return null;
        }
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    hmacKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }
}
