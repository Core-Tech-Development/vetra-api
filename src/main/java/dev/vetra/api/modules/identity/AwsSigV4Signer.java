package dev.vetra.api.modules.identity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Minimal AWS Signature Version 4 signer for SES API calls.
 * Uses only standard Java crypto — no external dependencies.
 */
public final class AwsSigV4Signer {

    private static final DateTimeFormatter AMZ_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private AwsSigV4Signer() {}

    /**
     * Signs an HTTP request with AWS Signature V4.
     *
     * @return map of headers to add to the request (authorization, x-amz-date, host, etc.)
     */
    public static Map<String, String> sign(
            String method,
            URI uri,
            String contentType,
            String body,
            String accessKeyId,
            String secretAccessKey,
            String region,
            String service) {

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String amzDate = now.format(AMZ_DATE_FMT);
        String dateStamp = now.format(DATE_STAMP_FMT);
        String credentialScope = dateStamp + "/" + region + "/" + service + "/aws4_request";

        String payloadHash = sha256Hex(body != null ? body : "");

        // Canonical headers (must be sorted by lowercase key)
        TreeMap<String, String> canonicalHeaderMap = new TreeMap<>();
        canonicalHeaderMap.put("content-type", contentType);
        canonicalHeaderMap.put("host", uri.getHost());
        canonicalHeaderMap.put("x-amz-date", amzDate);

        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeaderNames = new StringBuilder();
        for (var entry : canonicalHeaderMap.entrySet()) {
            canonicalHeaders.append(entry.getKey()).append(":").append(entry.getValue().trim()).append("\n");
            if (!signedHeaderNames.isEmpty()) signedHeaderNames.append(";");
            signedHeaderNames.append(entry.getKey());
        }

        // Canonical request
        String canonicalRequest = method + "\n"
                + uri.getRawPath() + "\n"
                + (uri.getRawQuery() != null ? uri.getRawQuery() : "") + "\n"
                + canonicalHeaders + "\n"
                + signedHeaderNames + "\n"
                + payloadHash;

        // String to sign
        String stringToSign = "AWS4-HMAC-SHA256\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);

        // Signing key
        byte[] kDate = hmacSha256(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, "aws4_request");

        String signature = hexEncode(hmacSha256(kSigning, stringToSign));

        String authorization = "AWS4-HMAC-SHA256 Credential=" + accessKeyId + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaderNames
                + ", Signature=" + signature;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("Host", uri.getHost());
        headers.put("X-Amz-Date", amzDate);
        headers.put("Authorization", authorization);
        return headers;
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return hexEncode(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 computation failed", e);
        }
    }

    private static String hexEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
