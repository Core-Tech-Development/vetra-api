package dev.vetra.api.shared.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * MinIO/S3-compatible storage configuration.
 * Mapped from application.properties under the prefix "vetra.minio".
 */
@ConfigMapping(prefix = "vetra.minio")
public interface MinioConfig {

    @WithDefault("http://localhost:9000")
    String endpoint();

    @WithDefault("minioadmin")
    String accessKey();

    @WithDefault("minioadmin")
    String secretKey();

    @WithDefault("vetra-exam-files")
    String bucket();
}
